package net.botwithus.kxapi.script

import com.google.gson.JsonObject
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.botwithus.rs3.client.Client
import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.kxapi.imgui.ImGuiDSL
import net.botwithus.kxapi.imgui.ImGuiUI
import net.botwithus.kxapi.permissive.PermissiveDSL
import net.botwithus.kxapi.permissive.StateEnum
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import net.botwithus.ui.workspace.Workspace
import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.ui.interfaces.BuildableUI
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * Base class for BotWithUs scripts that want Kotlin `suspend` semantics while still being scheduled on server ticks.
 *
 * The runtime calls [run] once per server tick. Internally this class owns a coroutine scope that executes script
 * work on a single-threaded dispatcher backed by the game tick. Implementations override [onLoop] and call helper
 * suspending functions such as [awaitTicks], [awaitUntil], and [awaitIdle] to pause their logic without blocking the
 * underlying scheduler. When the requested tick or condition is reached, the coroutine is resumed and execution
 * continues. Additional coroutines can be launched via [scriptScope] and coordinate with a central waiting queue so
 * multiple suspensions can coexist safely.
 */
abstract class SuspendableScript : BwuScript() {

    /**
     * Marker for suspendable scripts that expose a permissive state machine. Implementers can supply the enum
     * backing the machine and optionally override the defaults to customise behaviour.
     */
    interface StateMachine<State> where State : Enum<State>, State : StateEnum {
        val enableStateMachine: Boolean get() = true
        val defaultState: State? get() = null
        fun stateEnumClass(): Class<State>? = null
    }

    private val stateInstances = mutableMapOf<Enum<*>, PermissiveDSL<*>>()
    private var stateMachineReady = false

    private var configUi: ImGuiUI? = null
    private var workspaceDrawer: (suspend Workspace.() -> Unit)? = null
    private var saveHandler: (JsonObject.() -> Unit)? = null
    private var loadHandler: (JsonObject.() -> Unit)? = null

    /**
     * Controls whether the permissive state machine should boot automatically. Defaults to `false` unless the script
     * implements [StateMachine].
     */
    protected open val enableStateMachine: Boolean
        get() = (this as? StateMachine<*>)?.enableStateMachine ?: false

    /**
     * Override to customise the initial state. Defaults to the enum's first constant.
     */
    protected open val defaultStateEnum: Enum<*>?
        get() = (this as? StateMachine<*>)?.defaultState as? Enum<*>

    /**
     * Override to provide the enum backing the state machine. The default implementation attempts to resolve the
     * generic parameter declared on [StateMachine].
     */
    protected open fun stateEnumClass(): Class<out Enum<*>>? {
        (this as? StateMachine<*>)?.stateEnumClass()?.let { return it }
        return resolveStateClassFromHierarchy()
    }

    /**
     * Provides read-only access to the active permissive state instance, if a state machine is configured.
     */
    protected val activeState: PermissiveDSL<*>?
        get() {
            if (!stateMachineReady) return null
            val activeName = runCatching { currentState.name }.getOrNull() ?: return null
            return stateInstances.entries.firstOrNull { (enumConst, _) ->
                (enumConst as? StateEnum)?.description == activeName
            }?.value
        }

    /** Snapshot capturing minimal player activity information for idle checks. */
    data class PlayerActivitySnapshot(
        val captureTick: Int,
        val animationId: Int,
        val isMoving: Boolean?
    ) {
        val isAnimationIdle: Boolean get() = animationId == -1
        val isMovementIdle: Boolean get() = isMoving != true
    }

    /**
     * Result of awaiting player idleness. Provides success and timeout cases with diagnostic details.
     */
    sealed class IdleAwaitResult {
        data class Idle(val snapshot: PlayerActivitySnapshot) : IdleAwaitResult()
        data class Timeout(
            val snapshot: PlayerActivitySnapshot,
            val animationActive: Boolean,
            val movementActive: Boolean
        ) : IdleAwaitResult()
    }

    private data class WaitRequest(
        val continuation: CancellableContinuation<Unit>,
        val resumeTick: Int
    )

    private val pendingDispatches: ArrayDeque<Runnable> = ArrayDeque()
    private val pendingWaits = mutableListOf<WaitRequest>()

    /** Dispatches coroutine work onto the single-threaded tick queue so jobs run in lockstep with the script. */
    private val scriptDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pendingDispatches.addLast(block)
            if (logger.isDebugEnabled) {
                logger.debug("{} queued coroutine dispatch", javaClass.simpleName)
            }
        }
    }

    /** Captures uncaught coroutine exceptions and routes them through [handleCoroutineFailure]. */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleCoroutineFailure(throwable)
    }

    private var scopeJob: Job = SupervisorJob()
    private var internalScope = CoroutineScope(scopeJob + scriptDispatcher + exceptionHandler)

    /**
     * Coroutine scope bound to the script lifecycle. Use this to launch helper coroutines that should be
     * co-operatively scheduled on game ticks.
     */
    protected val scriptScope: CoroutineScope
        get() {
            if (!scopeJob.isActive) {
                if (logger.isDebugEnabled) {
                    logger.debug("{} recreating coroutine scope", javaClass.simpleName)
                }
                scopeJob = SupervisorJob()
                internalScope = CoroutineScope(scopeJob + scriptDispatcher + exceptionHandler)
            }
            return internalScope
        }

    /** Primary coroutine executing [onLoop]; recreated if it completes or the script restarts. */
    private var mainJob: Job? = null

    @Volatile
    private var isCancelled = false
    private var cancellationCause: CancellationException? = null

    /**
     * Main coroutine body invoked once per iteration. Override this instead of [run]; use the helper suspending
     * functions to pause between actions. The default implementation calls [awaitTicks] after each loop, so most
     * scripts simply perform their logic here.
     */
    protected open suspend fun onTick() {}

    open suspend fun onLoop() {
        onTick()
    }

    protected open suspend fun onDrawConfigSuspend(workspace: Workspace) {}

    protected open suspend fun buildUI(): BuildableUI? = configUi

    protected open fun saveData(data: JsonObject) {
        saveHandler?.invoke(data)
    }

    protected open fun loadData(data: JsonObject) {
        loadHandler?.invoke(data)
    }

    override fun onDrawConfig(p0: Workspace?) {
        if (p0 != null) {
            scriptScope.launch {
                workspaceDrawer?.invoke(p0)
                onDrawConfigSuspend(p0)
            }
        }
    }

    override fun getBuildableUI(): BuildableUI? {
        var ui: BuildableUI? = null
        runBlocking(scriptDispatcher) {
            ui = buildUI()
        }
        return ui
    }

    override fun savePersistentData(obj: JsonObject?) {
        if (obj != null) { saveData(obj) }
    }

    override fun loadPersistentData(obj: JsonObject?) {
        if (obj != null) { loadData(obj) }
    }

    override fun onInitialize() {
        super.onInitialize()
        logger.debug("{} initializing (stateMachineEnabled={})", javaClass.simpleName, enableStateMachine)
        stateInstances.clear()
        stateMachineReady = false
        if (enableStateMachine) {
            setupStateMachine()
        }
        onScriptReady()
    }

    /**
     * Entry point called by the BotWithUs runtime every tick. It drains any pending coroutine dispatches, resumes
     * waiters whose scheduled tick has arrived, and ensures the main coroutine is running. Consumers rarely need to
     * override this because [onLoop] provides the suspendable API surface.
     */
    override fun run() {
        if (isCancelled) return

        ensureScopeActive()
        drainDispatchQueue()
        resumeReadyWaiters(Client.getServerTick())
        drainDispatchQueue()
        ensureMainCoroutine()
        drainDispatchQueue()
    }

    override fun onActivation() {
        logger.debug("{} activating", javaClass.simpleName)
        resetCancellationState()
        super.onActivation()
        onScriptActivated()
    }

    override fun onDeactivation() {
        logger.debug("{} deactivating", javaClass.simpleName)
        onScriptDeactivated()
        cancelScript(CancellationException("Script deactivated"))
        super.onDeactivation()
    }

    protected open fun onScriptReady() {}

    protected open fun onScriptActivated() {}

    protected open fun onScriptDeactivated() {}

    @Suppress("UNCHECKED_CAST")
    protected open fun <State> switchToState(state: State) where State : Enum<State>, State : StateEnum {
        switchToStateInternal(state as Enum<*>, announce = true)
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun <State> getState(state: State): PermissiveDSL<*>? where State : Enum<State>, State : StateEnum {
        return instantiateState(state as Enum<*>)
    }

    protected open fun <State> isStateActive(state: State): Boolean where State : Enum<State>, State : StateEnum {
        if (!stateMachineReady) return false
        val activeName = runCatching { currentState.name }.getOrNull() ?: return false
        return activeName == state.description
    }

    protected fun configPanel(builder: ImGuiDSL.() -> Unit) {
        configUi = object : ImGuiUI() {
            override fun build(): ImGuiDSL.() -> Unit = builder
        }
        logger.debug("{} registered config panel builder", javaClass.simpleName)
    }

    protected fun configPanel(ui: ImGuiUI) {
        configUi = ui
        logger.debug("{} registered config panel", javaClass.simpleName)
    }

    protected fun workspacePanel(block: suspend Workspace.() -> Unit) {
        workspaceDrawer = block
        logger.debug("{} registered workspace panel", javaClass.simpleName)
    }

    protected fun onSaveState(block: JsonObject.() -> Unit) {
        saveHandler = block
        logger.debug("{} registered save-state handler", javaClass.simpleName)
    }

    protected fun onLoadState(block: JsonObject.() -> Unit) {
        loadHandler = block
        logger.debug("{} registered load-state handler", javaClass.simpleName)
    }

    protected fun persistentState(
        onLoad: JsonObject.() -> Unit,
        onSave: JsonObject.() -> Unit
    ) {
        loadHandler = onLoad
        saveHandler = onSave
        logger.debug("{} registered persistent state handlers", javaClass.simpleName)
    }

    private fun setupStateMachine() {
        if (stateMachineReady) {
            logger.debug("{} state machine already initialised", javaClass.simpleName)
            return
        }
        val enumClass = stateEnumClass() ?: run {
            logger.warn("State machine enabled but no enum type detected on {}", javaClass.simpleName)
            return
        }

        val enumConstants = enumClass.enumConstants as? Array<out Enum<*>> ?: emptyArray()
        val states = enumConstants.mapNotNull { instantiateState(it) }.toTypedArray()
        if (states.isEmpty()) {
            logger.warn("State machine enabled but no state instances could be created for {}", javaClass.simpleName)
            return
        }

        initStates(*states)
        stateMachineReady = true
        logger.debug("{} initialised state machine with {} state(s)", javaClass.simpleName, states.size)

        val initial = defaultStateEnum ?: enumConstants.firstOrNull()
        if (initial != null) {
            logger.debug("{} resolved initial state {}", javaClass.simpleName, stateDescription(initial))
            switchToStateInternal(initial, announce = false)
        } else {
            logger.debug("{} did not resolve an initial state", javaClass.simpleName)
        }
    }

    private fun instantiateState(enumConst: Enum<*>): PermissiveDSL<*>? {
        stateInstances[enumConst]?.let {
            logger.debug("{} using cached state {}", javaClass.simpleName, enumConst.name)
            return it
        }
        val descriptor = enumConst as? StateEnum ?: run {
            logger.warn("State {} does not implement StateEnum on {}", enumConst.name, javaClass.simpleName)
            return null
        }
        val stateClass = descriptor.classz
        val instance = runCatching {
            val constructor = stateClass.java.getDeclaredConstructor(this::class.java, String::class.java)
            constructor.isAccessible = true
            constructor.newInstance(this, descriptor.description)
        }.onFailure { error ->
            logger.error(
                "Failed to construct state {} ({}): {}",
                enumConst.name,
                stateClass.qualifiedName,
                error.message,
                error
            )
        }.getOrNull()
        if (instance != null) {
            stateInstances[enumConst] = instance
            logger.debug(
                "{} instantiated state {} ({})",
                javaClass.simpleName,
                enumConst.name,
                stateClass.qualifiedName
            )
        } else {
            logger.warn("State {} could not be instantiated for {}", enumConst.name, javaClass.simpleName)
        }
        return instance
    }

    private fun switchToStateInternal(state: Enum<*>, announce: Boolean) {
        if (!stateMachineReady) {
            logger.debug("{} requested state {} before state machine ready", javaClass.simpleName, state.name)
            instantiateState(state)
            return
        }
        val instance = instantiateState(state) ?: run {
            logger.warn("State {} could not be activated on {}", state.name, javaClass.simpleName)
            return
        }
        val description = stateDescription(state)
        setCurrentState(description)
        status = if (announce) {
            "Switched to $description"
        } else {
            "Active state: $description"
        }
        logger.debug("{} transitioned to state {}", javaClass.simpleName, instance.name)
    }

    private fun stateDescription(enumConst: Enum<*>): String =
        (enumConst as? StateEnum)?.description ?: enumConst.name

    private fun resolveStateClassFromHierarchy(): Class<out Enum<*>>? {
        var current: Class<*>? = this::class.java
        while (current != null) {
            current.genericInterfaces.forEach { type ->
                resolveStateClassFromType(type)?.let { return it }
            }
            resolveStateClassFromType(current.genericSuperclass)?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun resolveStateClassFromType(type: Type?): Class<out Enum<*>>? {
        if (type !is ParameterizedType) return null
        val raw = (type.rawType as? Class<*>) ?: return null
        if (raw != StateMachine::class.java) return null
        val actual = type.actualTypeArguments.firstOrNull()
        val enumClass = actual as? Class<*>
        return if (enumClass != null && Enum::class.java.isAssignableFrom(enumClass)) {
            @Suppress("UNCHECKED_CAST")
            enumClass as Class<out Enum<*>>
        } else {
            null
        }
    }

    /**
     * Suspends the calling coroutine for the requested number of server ticks.
     *
     * @param ticks Number of ticks to wait from the current server tick. A value of zero resumes within the same tick.
     * @throws IllegalArgumentException When a negative value is supplied.
     * @throws CancellationException If the script has been cancelled.
     */
    suspend fun awaitTicks(ticks: Int) {
        if (ticks < 0) {
            logger.error("awaitTicks received negative tick count {} on {}", ticks, javaClass.simpleName)
            throw IllegalArgumentException("ticks must be non-negative")
        }
        ensureActive()
        suspendCancellableCoroutine { cont ->
            if (isCancelled) {
                cont.cancel(cancellationCause ?: CancellationException("Script cancelled"))
                return@suspendCancellableCoroutine
            }
            val resumeTick = Client.getServerTick() + ticks
            if (logger.isDebugEnabled) {
                logger.debug("{} scheduling suspension for {} tick(s) (resume @ {})", javaClass.simpleName, ticks, resumeTick)
            }
            val request = WaitRequest(cont, resumeTick)
            pendingWaits.add(request)
            cont.invokeOnCancellation { pendingWaits.remove(request) }
        }
    }

    /**
     * Suspends until [condition] evaluates to true or the optional timeout is reached.
     *
     * @param timeout Maximum number of ticks to wait before giving up. Defaults to five ticks.
     * @param condition Predicate evaluated once per resume. Returning `true` resumes the coroutine immediately.
     * @return `true` when the condition fired before the timeout, otherwise `false`.
     * @throws IllegalArgumentException If a negative timeout is supplied.
     * @throws CancellationException If the script has been cancelled.
     */
    suspend fun awaitUntil(timeout: Int = 5, condition: () -> Boolean): Boolean {
        if (timeout < 0) {
            logger.error("awaitUntil received negative timeout {} on {}", timeout, javaClass.simpleName)
            throw IllegalArgumentException("timeout must be non-negative")
        }
        ensureActive()

        if (condition()) {
            logger.debug("{} condition satisfied immediately in awaitUntil", javaClass.simpleName)
            return true
        }
        val startTick = Client.getServerTick()
        val deadline = startTick + timeout
        if (logger.isDebugEnabled) {
            logger.debug("{} awaiting condition for up to {} tick(s)", javaClass.simpleName, timeout)
        }
        while (Client.getServerTick() < deadline) {
            awaitTicks(1)
            ensureActive()
            if (condition()) {
                logger.debug("{} condition satisfied after {} tick(s)", javaClass.simpleName, Client.getServerTick() - startTick)
                return true
            }
        }
        if (timeout > 0) {
            logger.warn("{} awaitUntil timed out after {} tick(s)", javaClass.simpleName, timeout)
        } else if (logger.isDebugEnabled) {
            logger.debug("{} awaitUntil timed out with zero timeout", javaClass.simpleName)
        }
        return false
    }

    /**
     * Suspends until the local player is idle or the timeout expires.
     *
     * @param timeout Maximum number of ticks to wait for idleness.
     * @param includeMovement When `true`, the player must not be moving; when `false`, only animation idleness is
     * validated.
     * @return `true` if the player became idle before the deadline, otherwise `false`.
     * @throws IllegalArgumentException If a negative timeout is supplied.
     * @throws CancellationException If the script has been cancelled.
     */
    suspend fun awaitIdle(timeout: Int = 10, includeMovement: Boolean = true): Boolean =
        when (awaitIdleState(timeout, includeMovement)) {
            is IdleAwaitResult.Idle -> true
            is IdleAwaitResult.Timeout -> false
        }

    /**
     * Suspends until the local player is idle, or emits a diagnostic payload describing why idleness was not reached.
     *
     * @param timeout Maximum number of ticks to wait for idleness.
     * @param includeMovement When `true`, the player must not be moving; when `false`, only animation idleness is
     * validated.
     * @return [IdleAwaitResult.Idle] when idle is observed, otherwise a [IdleAwaitResult.Timeout] containing activity
     * details.
     * @throws IllegalArgumentException If a negative timeout is supplied.
     * @throws CancellationException If the script has been cancelled.
     */
    suspend fun awaitIdleState(timeout: Int = 10, includeMovement: Boolean = true): IdleAwaitResult {
        if (timeout < 0) {
            logger.error("awaitIdleState received negative timeout {} on {}", timeout, javaClass.simpleName)
            throw IllegalArgumentException("timeout must be non-negative")
        }
        ensureActive()

        val startTick = Client.getServerTick()
        val deadline = startTick + timeout
        if (logger.isDebugEnabled) {
            logger.debug("{} awaiting player idle for up to {} tick(s) (includeMovement={})", javaClass.simpleName, timeout, includeMovement)
        }
        while (true) {
            val snapshot = capturePlayerActivity()
            val animationIdle = snapshot.isAnimationIdle
            val movementIdle = !includeMovement || snapshot.isMovementIdle
            if (animationIdle && movementIdle) {
                val waited = (snapshot.captureTick - startTick).coerceAtLeast(0)
                logger.debug("{} detected idle after {} tick(s)", javaClass.simpleName, waited)
                return IdleAwaitResult.Idle(snapshot)
            }
            if (Client.getServerTick() >= deadline) {
                val waited = (deadline - startTick).coerceAtLeast(0)
                logger.warn("{} idle wait timed out after {} tick(s) (animationActive={}, movementActive={})", javaClass.simpleName, waited, !animationIdle, includeMovement && !snapshot.isMovementIdle)
                return IdleAwaitResult.Timeout(
                    snapshot = snapshot,
                    animationActive = !animationIdle,
                    movementActive = includeMovement && !snapshot.isMovementIdle
                )
            }
            awaitTicks(1)
            ensureActive()
        }
    }

    /** Kotlin-facing alias for [awaitTicks] to match prior API naming while keeping the JVM signature distinct. */
    @JvmName("waitTicksAlias")
    @Suppress("unused")
    suspend fun wait(ticks: Int) = awaitTicks(ticks)

    /** Kotlin-facing alias for [awaitUntil] that mirrors the original example API. */
    @JvmName("waitUntilAlias")
    @Suppress("unused")
    suspend fun waitUntil(timeout: Int = 5, condition: () -> Boolean) = awaitUntil(timeout, condition)

    /**
     * Hook invoked when the primary coroutine throws an unhandled exception. The default implementation logs the
     * failure and cancels the entire script. Override to customise error reporting.
     */
    protected open fun handleCoroutineFailure(cause: Throwable) {
        logger.error("Unhandled coroutine failure in {}", javaClass.simpleName, cause)
        cancelScript(CancellationException("Coroutine failed").also { it.initCause(cause) })
    }

    /**
     * Cancels the script and tears down all associated coroutines. Subsequent calls to [run] become no-ops until the
     * script is reactivated.
     */
    protected fun cancelScript(cause: CancellationException = CancellationException("Script cancelled")) {
        if (isCancelled) return
        if (logger.isDebugEnabled) {
            logger.debug("{} cancelling script ({})", javaClass.simpleName, cause.message ?: "no message")
        }
        isCancelled = true
        cancellationCause = cause

        val waiters = pendingWaits.toList()
        pendingWaits.clear()
        waiters.forEach { it.continuation.cancel(cause) }

        pendingDispatches.clear()
        mainJob?.cancel(cause)
        mainJob = null
        scopeJob.cancel(cause)
    }

    /** Cooperative main loop that calls [onLoop] and yields once per tick. */
    private val coroutineBlock: suspend CoroutineScope.() -> Unit = {
        while (isActive) {
            onLoop()
            awaitTicks(1)
        }
    }

    /** Recreates the coroutine scope if it was cancelled so new work can be scheduled. */
    private fun ensureScopeActive() {
        scriptScope // getter reinitialises if needed
    }

    /** Lazily starts the main script coroutine when the runtime first ticks or after a restart. */
    private fun ensureMainCoroutine() {
        if (mainJob?.isActive == true || isCancelled) return
        logger.debug("{} starting main coroutine", javaClass.simpleName)
        mainJob = scriptScope.launch(block = coroutineBlock)
    }

    /** Records the local player state for later idle diagnostics. */
    private fun capturePlayerActivity(): PlayerActivitySnapshot {
        val tick = Client.getServerTick()
        val player = LocalPlayer.self()
        return PlayerActivitySnapshot(
            captureTick = tick,
            animationId = player?.animationId ?: -1,
            isMoving = player?.isMoving
        )
    }

    /** Runs queued coroutine continuations and tasks until the queue is empty or the script cancels. */
    private fun drainDispatchQueue() {
        if (pendingDispatches.isEmpty()) return
        val queued = pendingDispatches.size
        if (logger.isDebugEnabled) {
            logger.debug("{} draining {} dispatch task(s)", javaClass.simpleName, queued)
        }
        while (pendingDispatches.isNotEmpty() && !isCancelled) {
            pendingDispatches.removeFirst().run()
        }
    }

    /** Resumes suspended waiters whose scheduled wake-up tick has elapsed. */
    private fun resumeReadyWaiters(currentTick: Int) {
        if (pendingWaits.isEmpty() || isCancelled) return
        while (true) {
            val ready = pendingWaits.filter { currentTick >= it.resumeTick }
            if (ready.isEmpty() || isCancelled) return
            if (logger.isDebugEnabled) {
                logger.debug("{} resuming {} waiter(s) at tick {}", javaClass.simpleName, ready.size, currentTick)
            }
            ready.forEach { request ->
                pendingWaits.remove(request)
                request.continuation.resume(Unit)
            }
        }
    }

    /** Throws the stored cancellation cause, if any, so suspending helpers observe cancellation promptly. */
    private fun ensureActive() {
        cancellationCause?.let { throw it }
    }

    /** Clears cancellation state when the script activates so new runs start with a clean slate. */
    private fun resetCancellationState() {
        logger.debug("{} resetting cancellation state", javaClass.simpleName)
        isCancelled = false
        cancellationCause = null
        pendingWaits.clear()
        pendingDispatches.clear()
        mainJob = null
        ensureScopeActive()
    }
}

