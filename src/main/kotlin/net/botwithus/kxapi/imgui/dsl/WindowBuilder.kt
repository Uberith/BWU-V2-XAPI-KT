package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui windows with a fluent interface.
 * 
 * This class provides methods for configuring window properties and
 * executing content within the window context.
 * 
 * @author Mark
 * @since 1.0.0
 */
class WindowBuilder(private val name: String, private val flags: Int) {
    private var x = -1f
    private var y = -1f
    private var width = -1f
    private var height = -1f
    private var collapsed = false
    private var focused = false
    private var bgAlpha = -1f
    private var fontScale = 1.0f
    
    /**
     * Sets the window position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return This builder for method chaining
     */
    fun position(x: Float, y: Float) = apply {
        this.x = x
        this.y = y
    }
    
    /**
     * Sets the window size.
     * 
     * @param width Window width
     * @param height Window height
     * @return This builder for method chaining
     */
    fun size(width: Float, height: Float) = apply {
        this.width = width
        this.height = height
    }
    
    /**
     * Sets whether the window should be collapsed.
     * 
     * @param collapsed True if collapsed
     * @return This builder for method chaining
     */
    fun collapsed(collapsed: Boolean) = apply {
        this.collapsed = collapsed
    }
    
    /**
     * Sets whether the window should be focused.
     * 
     * @param focused True if focused
     * @return This builder for method chaining
     */
    fun focused(focused: Boolean) = apply {
        this.focused = focused
    }
    
    /**
     * Sets the background alpha value.
     * 
     * @param alpha Alpha value (0.0 to 1.0)
     * @return This builder for method chaining
     */
    fun bgAlpha(alpha: Float) = apply {
        this.bgAlpha = alpha
    }
    
    /**
     * Sets the font scale for the window.
     * 
     * @param scale Font scale multiplier
     * @return This builder for method chaining
     */
    fun fontScale(scale: Float) = apply {
        this.fontScale = scale
    }
    
    /**
     * Executes the content within the window context.
     * 
     * @param content The content to execute
     * @return True if the window is open, false otherwise
     */
    fun content(content: () -> Unit): Boolean {
        // Set window properties before opening
        if (x >= 0 && y >= 0) {
            ImGui.setNextWindowPos(x, y)
        }
        if (width > 0 && height > 0) {
            ImGui.setNextWindowSize(width, height)
        }
        if (collapsed) {
            ImGui.setNextWindowCollapsed(true)
        }
        if (focused) {
            ImGui.setNextWindowFocus()
        }
        if (bgAlpha >= 0) {
            ImGui.setNextWindowBgAlpha(bgAlpha)
        }
        
        // Open the window
        val isOpen = ImGui.begin(name, flags)
        
        if (isOpen) {
            // Set font scale if specified
            if (fontScale != 1.0f) {
                ImGui.setWindowFontScale(fontScale)
            }
            
            // Execute the content
            content()
        }
        
        // Always end the window
        ImGui.end()
        
        return isOpen
    }
}
