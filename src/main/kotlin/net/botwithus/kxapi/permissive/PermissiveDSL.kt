package net.botwithus.kxapi.permissive

import net.botwithus.kxapi.permissive.dsl.StateBuilder
import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.permissive.base.PermissiveScript
import net.botwithus.xapi.script.permissive.node.Branch
import org.slf4j.LoggerFactory

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

    private val logger = LoggerFactory.getLogger("${script.javaClass.simpleName}.$name")

    init {
        initializeNodes()
    }

    override fun initializeNodes() {
        val stateBuilder = StateBuilder(script)
        runCatching {
            stateBuilder.create()
            stateBuilder.build()
        }.onSuccess { built ->
            val branch = built as? Branch ?: run {
                val actualType = built?.javaClass?.name
                logger.error("State {} produced {} instead of Branch on {}", name, actualType, script.javaClass.simpleName)
                throw IllegalStateException("State $name must return a Branch, but received $actualType")
            }
            node = branch
            logger.debug("State {} initialised for {}", name, script.javaClass.simpleName)
        }.onFailure { throwable ->
            logger.error("Failed to initialize state {} on {}: {}", name, script.javaClass.simpleName, throwable.message ?: "unknown error", throwable)
            throw throwable
        }
    }

    protected abstract fun StateBuilder<T>.create()

}