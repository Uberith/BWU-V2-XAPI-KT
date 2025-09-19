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
    }

    private val stateInstances = mutableMapOf<State, PermissiveDSL<*>>()

    /**
     * Get or create an instance of the state for the given enum
     * @param stateEnum The enum value representing the state
     * @return The state instance
     */
    protected fun getState(stateEnum: State): PermissiveDSL<*>? {
        return stateInstances.getOrPut(stateEnum) {
            val stateClass = stateEnum.classz

            stateClass.java.getDeclaredConstructor(this::class.java, String::class.java)
                .newInstance(this, stateEnum.description)
                ?: throw IllegalStateException("Could not create instance of $stateClass. Make sure the constructor takes (script: BwuScript, name: String)")
        }
    }

    abstract fun init()

    /**
     * Switch to a specific state by enum value
     * @param stateEnum The state to switch to
     */
    fun switchToState(stateEnum: State) {
        getState(stateEnum)?.let { stateInstance ->
            setCurrentState(stateEnum.description)
            status = "Switched to ${stateEnum.description}"
        } ?: run {
            logger.error("State $stateEnum not found or could not be created")
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
        runCatching {
            init()

            val enumClass = this::class.java.genericSuperclass
                ?.let { it as? ParameterizedType }
                ?.actualTypeArguments
                ?.firstOrNull()
                ?.let { it as? Class<*> }
            
            val stateInstances = enumClass?.enumConstants?.mapNotNull { enumConstant ->
                getState(enumConstant as State)
            }?.toTypedArray() ?: emptyArray()
            
            // Call initStates with all state instances
            initStates(*stateInstances)
            status  = "Script initialized with state: ${currentState.name}"
        }.onFailure { e ->
            logger.error(e.message, e)
        }
    }

}