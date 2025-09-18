package net.botwithus.kxapi.permissive

import net.botwithus.kxapi.permissive.dsl.StateBuilder
import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.permissive.base.PermissiveScript
import net.botwithus.xapi.script.permissive.node.Branch

/**
 * Base class for states that provides a clean DSL interface for building state trees.
 *
 * PermissiveDSL extends PermissiveScript.State and hides all the boilerplate code
 * required for state initialization, allowing you to focus on defining your state logic
 * using the intuitive DSL syntax.
 *
 * @param T The type of BwuScript this state is for
 * @param script The script instance
 * @param name The unique name for this state
 *
 * @author Mark
 * @see <a href="https://github.com/Mark7625">GitHub Profile</a>
 * @since 1.0.0
 */
abstract class PermissiveDSL<T : BwuScript>(
    protected val script: T,
    name: String
) : PermissiveScript.State(name) {

    init {
        initializeNodes()
    }

    override fun initializeNodes() {
        val stateBuilder = StateBuilder(script)
        stateBuilder.create()
        node = stateBuilder.build() as Branch
        script.println("$name State Initialized")
    }

    protected abstract fun StateBuilder<T>.create()

}