package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui sections (collapsing headers) with a fluent interface.
 * 
 * This class provides methods for configuring section properties and
 * executing content within the section context.
 * 
 * @author Mark
 * @since 1.0.0
 */
class SectionBuilder(private val name: String, private val flags: Int = 0) : ConditionalDisplay {
    private var displayCondition: (() -> Boolean)? = null
    
    /**
     * Conditionally displays the section based on a boolean condition.
     * 
     * @param condition The condition to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: Boolean) = apply {
        this.displayCondition = { condition }
    }
    
    /**
     * Conditionally displays the section based on a condition function.
     * 
     * @param condition The condition function to check
     * @return This builder for method chaining
     */
    override fun displayWhen(condition: () -> Boolean) = apply {
        this.displayCondition = condition
    }
    
    /**
     * Executes the content within the section context.
     * 
     * @param content The content to execute
     * @return True if the section is open, false otherwise
     */
    fun content(content: () -> Unit): Boolean {
        // Check display condition first
        if (displayCondition != null && !displayCondition!!()) {
            return false
        }
        
        val isOpen = ImGui.collapsingHeader(name, flags)
        
        if (isOpen) {
            content()
        }
        
        return isOpen
    }
}
