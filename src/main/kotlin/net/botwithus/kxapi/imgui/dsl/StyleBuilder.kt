package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for managing ImGui styles with a fluent interface.
 * 
 * This class provides methods for configuring ImGui styling properties
 * and managing style state.
 * 
 * @author Mark
 * @since 1.0.0
 */
class StyleBuilder {
    
    /**
     * Pushes a style color and executes the content within that style context.
     * 
     * @param index Style color index
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     * @param content The content to execute within the style context
     */
    fun color(index: Int, r: Float, g: Float, b: Float, a: Float, content: () -> Unit) {
        ImGui.pushStyleColor(index, r, g, b, a)
        content()
        ImGui.popStyleColor(1)
    }
    
    /**
     * Pushes a style variable and executes the content within that style context.
     * 
     * @param index Style variable index
     * @param x First value
     * @param y Second value
     * @param content The content to execute within the style context
     */
    fun var_(index: Int, x: Float, y: Float, content: () -> Unit) {
        ImGui.pushStyleVar(index, x, y)
        content()
        ImGui.popStyleVar(1)
    }
    
    /**
     * Pushes item width and executes the content within that style context.
     * 
     * @param width Item width
     * @param content The content to execute within the style context
     */
    fun itemWidth(width: Float, content: () -> Unit) {
        ImGui.pushItemWidth(width)
        content()
        ImGui.popItemWidth()
    }
    
    /**
     * Pushes text wrap position and executes the content within that style context.
     * 
     * @param wrapPosX Text wrap position
     * @param content The content to execute within the style context
     */
    fun textWrapPos(wrapPosX: Float, content: () -> Unit) {
        ImGui.pushTextWrapPos(wrapPosX)
        content()
        ImGui.popTextWrapPos()
    }
    
    /**
     * Pushes allow keyboard focus and executes the content within that style context.
     * 
     * @param allowKeyboardFocus Whether to allow keyboard focus
     * @param content The content to execute within the style context
     */
    fun allowKeyboardFocus(allowKeyboardFocus: Boolean, content: () -> Unit) {
        ImGui.pushAllowKeyboardFocus(allowKeyboardFocus)
        content()
        ImGui.popAllowKeyboardFocus()
    }
    
    /**
     * Pushes button repeat and executes the content within that style context.
     * 
     * @param repeat Whether to repeat button presses
     * @param content The content to execute within the style context
     */
    fun buttonRepeat(repeat: Boolean, content: () -> Unit) {
        ImGui.pushButtonRepeat(repeat)
        content()
        ImGui.popButtonRepeat()
    }
    
    /**
     * Pushes item flag and executes the content within that style context.
     * 
     * @param itemFlag Item flag
     * @param enabled Whether the flag is enabled
     * @param content The content to execute within the style context
     */
    fun itemFlag(itemFlag: Int, enabled: Boolean, content: () -> Unit) {
        ImGui.pushItemFlag(itemFlag, enabled)
        content()
        ImGui.popItemFlag()
    }
}
