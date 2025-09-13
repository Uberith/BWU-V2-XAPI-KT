package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui child windows with a fluent interface.
 * 
 * This class provides methods for configuring child window properties and
 * managing child window content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class ChildWindowBuilder(private val id: String) {
    private var width = 0f
    private var height = 0f
    private var border = false
    private var flags = 0
    
    /**
     * Sets the child window size.
     * 
     * @param width Child window width
     * @param height Child window height
     * @return This builder for method chaining
     */
    fun size(width: Float, height: Float) = apply {
        this.width = width
        this.height = height
    }
    
    /**
     * Sets whether the child window has a border.
     * 
     * @param border True if bordered
     * @return This builder for method chaining
     */
    fun border(border: Boolean) = apply {
        this.border = border
    }
    
    /**
     * Sets the child window flags.
     * 
     * @param flags Child window flags
     * @return This builder for method chaining
     */
    fun flags(flags: Int) = apply {
        this.flags = flags
    }
    
    /**
     * Executes the content within the child window context.
     * 
     * @param content The content to execute within the child window
     * @return True if the child window is open
     */
    fun content(content: () -> Unit): Boolean {
        val isOpen = ImGui.beginChild(id, width, height, border, flags)
        
        if (isOpen) {
            content()
        }
        
        ImGui.endChild()
        return isOpen
    }
}
