package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui tab bars with a fluent interface.
 * 
 * This class provides methods for configuring tab bar properties and
 * managing tab content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class TabBarBuilder(private val id: String) {
    private var flags = 0
    
    /**
     * Sets the tab bar flags.
     * 
     * @param flags Tab bar flags
     * @return This builder for method chaining
     */
    fun flags(flags: Int) = apply {
        this.flags = flags
    }
    
    /**
     * Executes the content within the tab bar context.
     * 
     * @param content The content to execute within the tab bar
     * @return True if the tab bar was created successfully
     */
    fun content(content: () -> Unit): Boolean {
        val success = ImGui.beginTabBar(id, flags)
        
        if (success) {
            content()
        }
        
        ImGui.endTabBar()
        return success
    }
    
    companion object {
        /**
         * Creates a tab item within the tab bar.
         * 
         * @param label The tab label
         * @param content The content to execute within the tab
         * @return True if the tab is open
         */
        fun tab(label: String, content: () -> Unit): Boolean {
            return tab(label, 0, content)
        }
        
        /**
         * Creates a tab item within the tab bar with flags.
         * 
         * @param label The tab label
         * @param flags Tab flags
         * @param content The content to execute within the tab
         * @return True if the tab is open
         */
        fun tab(label: String, flags: Int, content: () -> Unit): Boolean {
            val isOpen = ImGui.beginTabItem(label, flags)
            
            if (isOpen) {
                content()
            }
            
            ImGui.endTabItem()
            return isOpen
        }
    }
}
