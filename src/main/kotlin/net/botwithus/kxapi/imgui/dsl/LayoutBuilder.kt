package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for managing ImGui layout with a fluent interface.
 * 
 * This class provides methods for configuring layout properties
 * and managing positioning and spacing.
 * 
 * @author Mark
 * @since 1.0.0
 */
class LayoutBuilder {
    
    /**
     * Sets the cursor position and executes the content.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param content The content to execute
     */
    fun cursorPos(x: Float, y: Float, content: () -> Unit) {
        ImGui.setCursorPos(x, y)
        content()
    }
    
    /**
     * Sets the cursor position X and executes the content.
     * 
     * @param x X coordinate
     * @param content The content to execute
     */
    fun cursorPosX(x: Float, content: () -> Unit) {
        ImGui.setCursorPosX(x)
        content()
    }
    
    /**
     * Sets the cursor position Y and executes the content.
     * 
     * @param y Y coordinate
     * @param content The content to execute
     */
    fun cursorPosY(y: Float, content: () -> Unit) {
        ImGui.setCursorPosY(y)
        content()
    }
    
    /**
     * Sets the cursor screen position and executes the content.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param content The content to execute
     */
    fun cursorScreenPos(x: Float, y: Float, content: () -> Unit) {
        ImGui.setCursorScreenPos(x, y)
        content()
    }
    
    /**
     * Adds a separator and executes the content.
     * 
     * @param content The content to execute
     */
    fun separator(content: () -> Unit) {
        ImGui.separator()
        content()
    }
    
    /**
     * Adds a separator with text and executes the content.
     * 
     * @param text The separator text
     * @param content The content to execute
     */
    fun separatorText(text: String, content: () -> Unit) {
        ImGui.separatorText(text)
        content()
    }
    
    /**
     * Adds spacing and executes the content.
     * 
     * @param content The content to execute
     */
    fun spacing(content: () -> Unit) {
        ImGui.spacing()
        content()
    }
    
    /**
     * Adds a new line and executes the content.
     * 
     * @param content The content to execute
     */
    fun newLine(content: () -> Unit) {
        ImGui.newLine()
        content()
    }
    
    /**
     * Adds a dummy element and executes the content.
     * 
     * @param width Dummy width
     * @param height Dummy height
     * @param content The content to execute
     */
    fun dummy(width: Float, height: Float, content: () -> Unit) {
        ImGui.dummy(width, height)
        content()
    }
    
    /**
     * Adds indentation and executes the content.
     * 
     * @param indentW Indentation width
     * @param content The content to execute
     */
    fun indent(indentW: Float, content: () -> Unit) {
        ImGui.indent(indentW)
        content()
        ImGui.unindent(indentW)
    }
    
    /**
     * Adds same line positioning and executes the content.
     * 
     * @param offsetFromStartX Offset from start X
     * @param spacing Spacing
     * @param content The content to execute
     */
    fun sameLine(offsetFromStartX: Float = 0f, spacing: Float = -1f, content: () -> Unit) {
        ImGui.sameLine(offsetFromStartX, spacing)
        content()
    }
}
