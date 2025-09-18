package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui menus with a fluent interface.
 * 
 * This class provides methods for configuring menu properties and
 * managing menu content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class MenuBuilder(private val label: String) {
    private var enabled = true
    
    /**
     * Sets whether the menu is enabled.
     * 
     * @param enabled True if enabled
     * @return This builder for method chaining
     */
    fun enabled(enabled: Boolean) = apply {
        this.enabled = enabled
    }
    
    /**
     * Executes the content within the menu context.
     * 
     * @param content The content to execute within the menu
     * @return True if the menu is open
     */
    fun content(content: () -> Unit): Boolean {
        val isOpen = ImGui.beginMenu(label, enabled)
        
        if (isOpen) {
            content()
        }
        
        ImGui.endMenu()
        return isOpen
    }
    
    companion object {
        /**
         * Creates a menu bar and executes the content within it.
         * 
         * @param content The content to execute within the menu bar
         * @return True if the menu bar was created successfully
         */
        fun menuBar(content: () -> Unit): Boolean {
            val success = ImGui.beginMenuBar()
            
            if (success) {
                content()
            }
            
            ImGui.endMenuBar()
            return success
        }
        
        /**
         * Creates a main menu bar and executes the content within it.
         * 
         * @param content The content to execute within the main menu bar
         * @return True if the main menu bar was created successfully
         */
        fun mainMenuBar(content: () -> Unit): Boolean {
            val success = ImGui.beginMainMenuBar()
            
            if (success) {
                content()
            }
            
            ImGui.endMainMenuBar()
            return success
        }
        
        /**
         * Creates a menu item with the specified properties.
         * 
         * @param label The menu item label
         * @param shortcut The keyboard shortcut (can be null)
         * @param selected Whether the item is selected
         * @param enabled Whether the item is enabled
         * @return True if the item was clicked
         */
        fun item(label: String, shortcut: String? = null, selected: Boolean = false, enabled: Boolean = true): Boolean {
            return ImGui.menuItem(label, shortcut, selected, enabled)
        }
    }
}
