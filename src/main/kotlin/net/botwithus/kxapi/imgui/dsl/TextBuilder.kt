package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui
import java.awt.Color

/**
 * Builder for creating ImGui text components with various styling options.
 * 
 * This class provides methods for configuring text display properties and
 * rendering different types of text elements.
 * 
 * @author Mark
 * @since 1.0.0
 */
class TextBuilder : ConditionalDisplay {
    private var text = ""
    private var r = 1.0f
    private var g = 1.0f
    private var b = 1.0f
    private var a = 1.0f
    private var disabled = false
    private var wrapped = false
    private var bullet = false
    private var label: String? = null
    private var shouldDisplay = true
    
    /**
     * Sets the text content.
     * 
     * @param text The text to display
     * @return This builder for method chaining
     */
    fun text(text: String) = apply {
        this.text = text
    }
    
    /**
     * Sets the text color.
     * 
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     * @return This builder for method chaining
     */
    fun colored(r: Float, g: Float, b: Float, a: Float) = apply {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }
    
    /**
     * Sets the text color using a single color value.
     * 
     * @param color Color value (0xRRGGBBAA format)
     * @return This builder for method chaining
     */
    fun colored(color: Int) = apply {
        this.r = ((color shr 24) and 0xFF) / 255.0f
        this.g = ((color shr 16) and 0xFF) / 255.0f
        this.b = ((color shr 8) and 0xFF) / 255.0f
        this.a = (color and 0xFF) / 255.0f
    }

    /**
     * Sets the text color using an AWT Color object.
     *
     * @param color The AWT Color object
     * @return This builder for method chaining
     */
    fun colored(color: Color) = apply {
        this.r = color.red / 255.0f
        this.g = color.green / 255.0f
        this.b = color.blue / 255.0f
        this.a = color.alpha / 255.0f
    }

    /**
     * Sets the text color using RGB values (alpha defaults to 1.0).
     * 
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @return This builder for method chaining
     */
    fun colored(r: Float, g: Float, b: Float) = apply {
        this.r = r
        this.g = g
        this.b = b
        this.a = 1.0f
    }
    
    /**
     * Makes the text appear disabled.
     * 
     * @return This builder for method chaining
     */
    fun disabled() = apply {
        this.disabled = true
    }
    
    /**
     * Makes the text wrap to multiple lines.
     * 
     * @return This builder for method chaining
     */
    fun wrapped() = apply {
        this.wrapped = true
    }
    
    /**
     * Makes the text appear as a bullet point.
     * 
     * @return This builder for method chaining
     */
    fun bullet() = apply {
        this.bullet = true
    }
    
    /**
     * Sets a label for the text (creates label-text pair).
     * 
     * @param label The label text
     * @return This builder for method chaining
     */
    fun label(label: String) = apply {
        this.label = label
    }
    
    /**
     * Conditionally displays the text based on a boolean condition.
     * 
     * @param condition The condition to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: Boolean) = apply {
        this.shouldDisplay = condition
    }
    
    /**
     * Conditionally displays the text based on a condition function.
     * 
     * @param condition The condition function to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: () -> Boolean) = apply {
        this.shouldDisplay = condition()
    }
    
    /**
     * Renders the text with the current configuration.
     */
    fun render() {
        if (!shouldDisplay) {
            return // Don't render anything if condition is false
        }
        
        when {
            label != null -> ImGui.labelText(label!!, text)
            bullet -> ImGui.bulletText(text)
            disabled -> ImGui.textDisabled(text)
            wrapped -> ImGui.textWrapped(text)
            r != 1.0f || g != 1.0f || b != 1.0f || a != 1.0f -> ImGui.textColored(text, r, g, b, a)
            else -> ImGui.text(text)
        }
    }
    
    /**
     * DSL-style configuration with a block.
     * 
     * @param text The text content
     * @param block Configuration block
     */
    operator fun invoke(text: String, block: TextBuilder.() -> Unit) {
        this.text = text
        block()
        render()
    }
    
    /**
     * DSL-style configuration with a block (no text parameter).
     * 
     * @param block Configuration block
     */
    operator fun invoke(block: TextBuilder.() -> Unit) {
        block()
        render()
    }
    
    companion object {
        /**
         * Creates a text component with the specified text and renders it immediately.
         * 
         * @param text The text to display
         * @return A new TextBuilder instance
         */
        fun of(text: String) = TextBuilder().text(text)
        
        /**
         * Creates a colored text component and renders it immediately.
         * 
         * @param text The text to display
         * @param r Red component (0.0 to 1.0)
         * @param g Green component (0.0 to 1.0)
         * @param b Blue component (0.0 to 1.0)
         * @param a Alpha component (0.0 to 1.0)
         * @return A new TextBuilder instance
         */
        fun colored(text: String, r: Float, g: Float, b: Float, a: Float) = 
            TextBuilder().text(text).colored(r, g, b, a)
        
        /**
         * Creates a disabled text component and renders it immediately.
         * 
         * @param text The text to display
         * @return A new TextBuilder instance
         */
        fun disabled(text: String) = TextBuilder().text(text).disabled()
        
        /**
         * Creates a wrapped text component and renders it immediately.
         * 
         * @param text The text to display
         * @return A new TextBuilder instance
         */
        fun wrapped(text: String) = TextBuilder().text(text).wrapped()
        
        /**
         * Creates a bullet text component and renders it immediately.
         * 
         * @param text The text to display
         * @return A new TextBuilder instance
         */
        fun bullet(text: String) = TextBuilder().text(text).bullet()
        
        /**
         * Creates a colored text component using an AWT Color object and renders it immediately.
         *
         * @param text The text to display
         * @param color The AWT Color object
         * @return A new TextBuilder instance
         */
        fun colored(text: String, color: Color) =
            TextBuilder().text(text).colored(color)

        /**
         * Creates a colored text component using RGB values and renders it immediately.
         * 
         * @param text The text to display
         * @param r Red component (0.0 to 1.0)
         * @param g Green component (0.0 to 1.0)
         * @param b Blue component (0.0 to 1.0)
         * @return A new TextBuilder instance
         */
        fun colored(text: String, r: Float, g: Float, b: Float) = 
            TextBuilder().text(text).colored(r, g, b)
    }
}
