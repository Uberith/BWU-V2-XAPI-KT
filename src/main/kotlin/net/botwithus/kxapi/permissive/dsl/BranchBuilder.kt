package net.botwithus.kxapi.permissive.dsl

import net.botwithus.kxapi.util.leafNode
import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.permissive.Interlock
import net.botwithus.xapi.script.permissive.Permissive
import net.botwithus.xapi.script.permissive.node.Branch
import net.botwithus.xapi.script.permissive.node.TreeNode

/**
 * Builder for creating conditional branches in the permissive state DSL.
 * 
 * BranchBuilder provides a fluent interface for defining branches with success and failure paths.
 * It supports both simple boolean conditions and complex Interlock-based conditions.
 *
 * 
 * @param T The type of BwuScript this builder is for
 * @param script The script instance to use for node creation
 * @param name The unique name for this branch
 * @param condition The condition function that determines success/failure
 * 
 * @author Mark
 * @see <a href="https://github.com/Mark7625">GitHub Profile</a>
 * @since 1.0.0
 */
@BranchDsl
class BranchBuilder<T : BwuScript>(val script: T, private val name: String, private var condition: () -> Boolean) {
    private var successNode: Any? = null
    private var failureNode: Any? = null
    private var customInterlock: Interlock? = null

    /**
     * Sets the success path to reference a node by type-safe name.
     * 
     * @param nodeName The type-safe name of the node to execute on success
     * 
     */
    fun onSuccess(nodeName: BranchName) {
        successNode = nodeName.value
    }

    /**
     * Sets the success path to reference a leaf by type-safe name.
     * 
     * @param nodeName The type-safe leaf name to execute on success
     * 
     */
    fun onSuccess(nodeName: LeafName) {
        successNode = nodeName.value
    }

    /**
     * Sets the success path to use a TreeNode instance directly.
     * 
     * @param node The TreeNode instance to execute on success
     * 
     */
    fun onSuccess(node: TreeNode) {
        successNode = node
    }

    /**
     * Sets the failure path to reference a node by type-safe name.
     * 
     * @param nodeName The type-safe name of the node to execute on failure
     * 
     */
    fun onFailure(nodeName: BranchName) {
        failureNode = nodeName.value
    }

    /**
     * Sets the failure path to reference a leaf by type-safe name.
     * 
     * @param nodeName The type-safe leaf name to execute on failure
     * 
     */
    fun onFailure(nodeName: LeafName) {
        failureNode = nodeName.value
    }

    /**
     * Sets the failure path to use a TreeNode instance directly.
     * 
     * @param node The TreeNode instance to execute on failure
     * 
     */
    fun onFailure(node: TreeNode) {
        failureNode = node
    }

    /**
     * Sets the success path to execute an inline action with type-safe leaf name.
     * 
     * This creates a leaf node with the specified action and uses it as the success path.
     * The action will be executed when the branch condition evaluates to true.
     * 
     * @param leafName The type-safe name for the generated leaf node
     * @param action The action to execute on success
     * 
     */
    fun onSuccess(leafName: LeafName, action: (T) -> Unit) {
        val leaf = leafNode(leafName.value, script) { action(script) }
        successNode = leaf
    }

    /**
     * Sets the failure path to execute an inline action with type-safe leaf name.
     * 
     * This creates a leaf node with the specified action and uses it as the failure path.
     * The action will be executed when the branch condition evaluates to false.
     * 
     * @param leafName The type-safe name for the generated leaf node
     * @param action The action to execute on failure
     * 
     */
    fun onFailure(leafName: LeafName, action: (T) -> Unit) {
        val leaf = leafNode(leafName.value, script) { action(script) }
        failureNode = leaf
    }

    /**
     * Overrides the default condition function for this branch.
     * 
     * @param customCondition A function that takes the script instance and returns a boolean
     * 
     */
    fun condition(customCondition: (T) -> Boolean) {
        condition = { customCondition(script) }
    }

    /**
     * Sets a custom Interlock for complex conditional logic.
     * 
     * @param interlock The Interlock containing multiple conditions
     * 
     */
    fun setInterlock(interlock: Interlock) {
        customInterlock = interlock
    }

    /**
     * Builds and returns the Branch node for this builder.
     * 
     * This method creates a Branch with either the custom Interlock (if set) or
     * a default Interlock using the condition function.
     * 
     * @return The built Branch node
     * 
     */
    fun build(): Branch {
        val interlock = customInterlock ?: Interlock(name, Permissive(name, condition))
        return Branch(script, name, interlock)
    }

    /**
     * Gets the success node for this branch.
     * 
     * @return The success node (String name, TreeNode instance, or null)
     */
    fun getSuccessNode(): Any? = successNode

    /**
     * Gets the failure node for this branch.
     * 
     * @return The failure node (String name, TreeNode instance, or null)
     */
    fun getFailureNode(): Any? = failureNode
}
