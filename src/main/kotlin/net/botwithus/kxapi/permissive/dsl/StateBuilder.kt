package net.botwithus.kxapi.permissive.dsl

import net.botwithus.kxapi.util.leafNode
import net.botwithus.xapi.script.BwuScript
import net.botwithus.xapi.script.permissive.Interlock
import net.botwithus.xapi.script.permissive.node.Branch
import net.botwithus.xapi.script.permissive.node.TreeNode

/**
 * Main DSL builder for constructing state trees in the permissive framework.
 * 
 * The StateBuilder provides a fluent, type-safe interface for defining complex state machines
 * with branches, leaves, and conditional logic. It handles the creation and wiring of all
 * state tree nodes automatically.
 *
 * @param T The type of BwuScript this builder is for
 * @param script The script instance to use for node creation
 * 
 * @author Mark
 * @see <a href="https://github.com/Mark7625">GitHub Profile</a>
 * @since 1.0.0
 */
@StateDsl
class StateBuilder<T : BwuScript>(val script: T) {
    private val nodes = mutableMapOf<String, Any>()
    private val branchBuilders = mutableMapOf<String, BranchBuilder<T>>()
    private var rootNode: String? = null

    /**
     * Creates a conditional branch with a type-safe branch name.
     * 
     * @param name The type-safe branch name
     * @param condition A function that returns true for success, false for failure
     * @param block DSL block for configuring the branch's success/failure paths
     * 
     */
    fun branch(name: BranchName, condition: () -> Boolean, block: BranchBuilder<T>.() -> Unit) {
        val branchBuilder = BranchBuilder(script, name.value, condition)
        branchBuilder.block()
        branchBuilders[name.value] = branchBuilder
    }

    /**
     * Creates a branch with a custom condition function.
     * 
     * @param name The unique name for this branch
     * @param block DSL block for configuring the branch's success/failure paths
     * 
     */
    fun branch(name: BranchName, block: BranchBuilder<T>.() -> Unit) {
        val branchBuilder = BranchBuilder(script, name.value, { false }) // Default condition
        branchBuilder.block()
        branchBuilders[name.value] = branchBuilder
    }

    /**
     * Creates a branch with an Interlock for complex conditional logic.
     * 
     * @param name The unique name for this branch
     * @param interlock The Interlock containing multiple conditions
     * @param block DSL block for configuring the branch's success/failure paths
     * 
     */

    fun branch(name: BranchName, interlock: Interlock, block: BranchBuilder<T>.() -> Unit) {
        val branchBuilder = BranchBuilder(script, name.value, { false }) // Default condition, will be overridden
        branchBuilder.setInterlock(interlock)
        branchBuilder.block()
        branchBuilders[name.value] = branchBuilder
    }

    /**
     * Creates a leaf node with a type-safe leaf name.
     * 
     * @param name The type-safe leaf name
     * @param action The action to execute when this leaf is reached
     * 
     */
    fun leaf(name: LeafName, action: (T) -> Unit) {
        nodes[name.value] = leafNode(name.value, script) { action(script) }
    }

    /**
     * Sets the root node of the state tree with a type-safe name.
     * 
     * @param nodeName The type-safe name of the node to use as the root
     * 
     */
    fun root(nodeName: BranchName) {
        rootNode = nodeName.value
    }

    /**
     * Sets the root node of the state tree using a TreeNode instance.
     * 
     * @param node The TreeNode instance to use as the root
     * 
     */
    fun root(node: TreeNode) {
        rootNode = "external_${System.currentTimeMillis()}"
        nodes[rootNode!!] = node
    }

    internal fun build(): Any? {
        branchBuilders.forEach { (name, builder) ->
            nodes[name] = builder.build()
        }

        // Then wire up the connections
        branchBuilders.forEach { (name, builder) ->
            val branch = nodes[name] as Branch
            val successNode = builder.getSuccessNode()
            val failureNode = builder.getFailureNode()

            val successChild = when (successNode) {
                is String -> nodes[successNode] as? TreeNode
                is TreeNode -> successNode
                else -> null
            }

            val failureChild = when (failureNode) {
                is String -> nodes[failureNode] as? TreeNode
                is TreeNode -> failureNode
                else -> null
            }

            if (successChild != null && failureChild != null) {
                branch.setChildrenNodes(successChild, failureChild)
            }
        }

        return rootNode?.let { nodes[it] }
    }
}