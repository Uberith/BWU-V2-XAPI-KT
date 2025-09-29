package net.botwithus.kxapi.permissive

import net.botwithus.xapi.script.BwuScript
import java.lang.reflect.ParameterizedType

/**
 * Enhanced PermissiveScript that supports enum-based state management with embedded class references.
 * 
 * This class works with enums that implement the StateEnum interface, ensuring they contain
 * both a description and a class reference that extends PermissiveDSL.
 *
 * @param State The enum class that implements StateEnum interface
 * 
 * @author Mark
 * @since 1.0.0
 */
abstract class PermissiveScript<State>(debug: Boolean) : BwuScript() where State : Enum<State>, State : StateEnum {

    init {
        isDebugMode = debug
        logger.debug("{} debug mode set to {}", javaClass.simpleName, debug)
    }

    private val stateInstances = mutableMapOf<State, PermissiveDSL<*>>()

    /**
     * Get or create an instance of the state for the given enum
     * @param stateEnum The enum value representing the state
     * @return The state instance
     */
    protected fun getState(stateEnum: State): PermissiveDSL<*>? {
        stateInstances[stateEnum]?.let {
            logger.debug("{} reusing state {}", javaClass.simpleName, stateEnum.description)
            return it
        }

        val stateClass = stateEnum.classz
        val instance = runCatching {
            val constructor = stateClass.java.getDeclaredConstructor(this::class.java, String::class.java)
            constructor.isAccessible = true
            constructor.newInstance(this, stateEnum.description)
        }.onFailure { error ->
            logger.error("Failed to create state {} for {}", stateEnum.name, javaClass.simpleName, error)
        }.getOrNull()

        if (instance != null) {
            stateInstances[stateEnum] = instance
            logger.debug(
                "{} instantiated state {} ({})",
                javaClass.simpleName,
                stateEnum.description,
                stateClass.qualifiedName
            )
        }
        return instance
    }

    abstract fun init()

    /**
     * Switch to a specific state by enum value
     * @param stateEnum The state to switch to
     */
    fun switchToState(stateEnum: State) {
        getState(stateEnum)?.let {
            setCurrentState(stateEnum.description)
            status = "Switched to ${stateEnum.description}"
            logger.debug("{} switched to state {}", javaClass.simpleName, stateEnum.description)
        } ?: run {
            logger.error("State {} not found or could not be created on {}", stateEnum.name, javaClass.simpleName)
        }
    }
    
    /**
     * Get a state instance by enum value
     * @param stateEnum The state enum to get
     * @return The state instance or null if not found
     */
    protected fun getStateInstance(stateEnum: State): PermissiveDSL<*>? {
        return getState(stateEnum)
    }
    
    /**
     * Check if a state is currently active
     * @param stateEnum The state to check
     * @return True if the state is currently active
     */
    protected fun isStateActive(stateEnum: State): Boolean {
        return currentState.name == stateEnum.description
    }

    override fun onInitialize() {
        super.onInitialize()
        logger.debug("{} initializing", javaClass.simpleName)
        runCatching {
            init()

            val enumClass = this::class.java.genericSuperclass
                ?.let { it as? ParameterizedType }
                ?.actualTypeArguments
                ?.firstOrNull()
                ?.let { it as? Class<*> }

            val resolvedStates = enumClass?.enumConstants?.mapNotNull { enumConstant ->
                getState(enumConstant as State)
            }?.toTypedArray() ?: emptyArray()

            // Call initStates with all state instances
            initStates(*resolvedStates)
            status  = "Script initialized with state: ${currentState.name}"
            if (resolvedStates.isEmpty()) {
                logger.warn("{} initialized without any resolvable states", javaClass.simpleName)
            } else {
                logger.debug("{} initialised with {} state(s); active={}", javaClass.simpleName, resolvedStates.size, currentState.name)
            }
        }.onFailure { e ->
            logger.error("Failed to initialize {}: {}", javaClass.simpleName, e.message ?: "unknown error", e)
        }
    }

}