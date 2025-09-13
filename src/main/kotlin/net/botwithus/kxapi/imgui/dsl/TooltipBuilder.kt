package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui tooltips with a fluent interface.
 * 
 * This class provides methods for configuring tooltip properties and
 * managing tooltip content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class TooltipBuilder {
    private var text: String? = null
    
    /**
     * Sets the tooltip text.
     * 
     * @param text The tooltip text
     * @return This builder for method chaining
     */
    fun text(text: String) = apply {
        this.text = text
    }
    
    /**
     * Executes the content within the tooltip context.
     * 
     * @param content The content to execute within the tooltip
     */
    fun content(content: () -> Unit) {
        ImGui.beginTooltip()
        content()
        ImGui.endTooltip()
    }
    
    /**
     * Renders the tooltip with the current text.
     */
    fun render() {
        text?.let { ImGui.setTooltip(it) }
    }
    
    companion object {
        /**
         * Creates a tooltip with the specified text and renders it immediately.
         * 
         * @param text The tooltip text
         * @return A new TooltipBuilder instance
         */
        fun of(text: String) = TooltipBuilder().text(text)
    }
}
