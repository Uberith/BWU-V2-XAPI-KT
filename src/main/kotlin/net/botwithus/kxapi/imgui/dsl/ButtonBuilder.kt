package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui button components with various styling options.
 * 
 * This class provides methods for configuring button properties and
 * handling button interactions.
 * 
 * @author Mark
 * @since 1.0.0
 */
class ButtonBuilder(private var label: String) : ConditionalDisplay {
    private var width = 0f
    private var height = 0f
    private var small = false
    private var invisible = false
    private var invisibleFlags = 0
    
    /**
     * Sets the button size.
     * 
     * @param width Button width
     * @param height Button height
     * @return This builder for method chaining
     */
    fun size(width: Float, height: Float) = apply {
        this.width = width
        this.height = height
    }
    
    /**
     * Makes the button small.
     * 
     * @return This builder for method chaining
     */
    fun small() = apply {
        this.small = true
    }
    
    /**
     * Makes the button invisible.
     * 
     * @return This builder for method chaining
     */
    fun invisible() = apply {
        this.invisible = true
    }
    
    /**
     * Makes the button invisible with specific flags.
     * 
     * @param flags Invisible button flags
     * @return This builder for method chaining
     */
    fun invisible(flags: Int) = apply {
        this.invisible = true
        this.invisibleFlags = flags
    }
    
    /**
     * Conditionally displays the button based on a boolean condition.
     * 
     * @param condition The condition to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: Boolean) = apply {
        if (!condition) {
            this.label = ""
        }
    }
    
    /**
     * Conditionally displays the button based on a condition function.
     * 
     * @param condition The condition function to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: () -> Boolean) = apply {
        if (!condition()) {
            this.label = ""
        }
    }
    
    /**
     * Renders the button and returns whether it was clicked.
     * 
     * @return True if the button was clicked, false otherwise
     */
    fun render(): Boolean {
        return when {
            invisible -> ImGui.invisibleButton(label, width, height, invisibleFlags)
            small -> ImGui.smallButton(label)
            else -> ImGui.button(label, width, height)
        }
    }
    
    /**
     * Renders the button and executes the action if clicked.
     * 
     * @param action The action to execute when clicked
     * @return True if the button was clicked, false otherwise
     */
    fun onClick(action: () -> Unit): Boolean {
        val clicked = render()
        if (clicked) {
            action()
        }
        return clicked
    }
    
    companion object {
        /**
         * Creates a button with the specified label and renders it immediately.
         * 
         * @param label The button label
         * @return A new ButtonBuilder instance
         */
        fun of(label: String) = ButtonBuilder(label)
        
        /**
         * Creates a small button with the specified label and renders it immediately.
         * 
         * @param label The button label
         * @return A new ButtonBuilder instance
         */
        fun small(label: String) = ButtonBuilder(label).small()
        
        /**
         * Creates an invisible button with the specified label and renders it immediately.
         * 
         * @param label The button label
         * @return A new ButtonBuilder instance
         */
        fun invisible(label: String) = ButtonBuilder(label).invisible()
    }
}
