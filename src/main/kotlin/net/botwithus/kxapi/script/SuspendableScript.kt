package net.botwithus.kxapi.script

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.botwithus.rs3.client.Client
import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.scripts.Script
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

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
abstract class SuspendableScript : Script() {

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

    private val logger = LoggerFactory.getLogger(javaClass)

    private val pendingDispatches: ArrayDeque<Runnable> = ArrayDeque()
    private val pendingWaits = mutableListOf<WaitRequest>()

    private val scriptDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pendingDispatches.addLast(block)
        }
    }

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
                scopeJob = SupervisorJob()
                internalScope = CoroutineScope(scopeJob + scriptDispatcher + exceptionHandler)
            }
            return internalScope
        }

    private var mainJob: Job? = null

    @Volatile
    private var isCancelled = false
    private var cancellationCause: CancellationException? = null

    /**
     * Main coroutine body invoked once per iteration. Override this instead of [run]; use the helper suspending
     * functions to pause between actions. The default implementation calls [awaitTicks] after each loop, so most
     * scripts simply perform their logic here.
     */
    abstract suspend fun onLoop()

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
        resetCancellationState()
        super.onActivation()
    }

    override fun onDeactivation() {
        cancelScript(CancellationException("Script deactivated"))
        super.onDeactivation()
    }

    /**
     * Suspends the calling coroutine for the requested number of server ticks.
     *
     * @param ticks Number of ticks to wait from the current server tick. A value of zero resumes within the same tick.
     * @throws IllegalArgumentException When a negative value is supplied.
     * @throws CancellationException If the script has been cancelled.
     */
    suspend fun awaitTicks(ticks: Int) {
        require(ticks >= 0) { "ticks must be non-negative" }
        ensureActive()
        suspendCancellableCoroutine { cont ->
            if (isCancelled) {
                cont.cancel(cancellationCause ?: CancellationException("Script cancelled"))
                return@suspendCancellableCoroutine
            }
            val resumeTick = Client.getServerTick() + ticks
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
        require(timeout >= 0) { "timeout must be non-negative" }
        ensureActive()

        if (condition()) return true
        val deadline = Client.getServerTick() + timeout
        while (Client.getServerTick() < deadline) {
            awaitTicks(1)
            ensureActive()
            if (condition()) return true
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
        require(timeout >= 0) { "timeout must be non-negative" }
        ensureActive()

        val deadline = Client.getServerTick() + timeout
        while (true) {
            val snapshot = capturePlayerActivity()
            val animationIdle = snapshot.isAnimationIdle
            val movementIdle = !includeMovement || snapshot.isMovementIdle
            if (animationIdle && movementIdle) {
                return IdleAwaitResult.Idle(snapshot)
            }
            if (Client.getServerTick() >= deadline) {
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

    private val coroutineBlock: suspend CoroutineScope.() -> Unit = {
        while (isActive) {
            onLoop()
            awaitTicks(1)
        }
    }

    private fun ensureScopeActive() {
        scriptScope // getter reinitialises if needed
    }

    private fun ensureMainCoroutine() {
        if (mainJob?.isActive == true || isCancelled) return
        mainJob = scriptScope.launch(block = coroutineBlock)
    }

    private fun capturePlayerActivity(): PlayerActivitySnapshot {
        val tick = Client.getServerTick()
        val player = LocalPlayer.self()
        return PlayerActivitySnapshot(
            captureTick = tick,
            animationId = player?.animationId ?: -1,
            isMoving = player?.isMoving
        )
    }

    private fun drainDispatchQueue() {
        if (pendingDispatches.isEmpty()) return
        while (pendingDispatches.isNotEmpty() && !isCancelled) {
            pendingDispatches.removeFirst().run()
        }
    }

    private fun resumeReadyWaiters(currentTick: Int) {
        if (pendingWaits.isEmpty() || isCancelled) return
        while (true) {
            val ready = pendingWaits.filter { currentTick >= it.resumeTick }
            if (ready.isEmpty() || isCancelled) return
            ready.forEach { request ->
                pendingWaits.remove(request)
                request.continuation.resume(Unit) { }
            }
        }
    }

    private fun ensureActive() {
        cancellationCause?.let { throw it }
    }

    private fun resetCancellationState() {
        isCancelled = false
        cancellationCause = null
        pendingWaits.clear()
        pendingDispatches.clear()
        mainJob = null
        ensureScopeActive()
    }
}
