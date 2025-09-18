package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui tree nodes with a fluent interface.
 * 
 * This class provides methods for configuring tree node properties and
 * managing tree node content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class TreeNodeBuilder(private val label: String) {
    private var flags = 0
    
    /**
     * Sets the tree node flags.
     * 
     * @param flags Tree node flags
     * @return This builder for method chaining
     */
    fun flags(flags: Int) = apply {
        this.flags = flags
    }
    
    /**
     * Executes the content within the tree node context.
     * 
     * @param content The content to execute within the tree node
     * @return True if the tree node is open
     */
    fun content(content: () -> Unit): Boolean {
        val isOpen = ImGui.treeNode(label, flags)
        
        if (isOpen) {
            content()
            ImGui.treePop()
        }
        
        return isOpen
    }
}
