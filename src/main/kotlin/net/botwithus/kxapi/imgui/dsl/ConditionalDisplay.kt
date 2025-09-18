package net.botwithus.kxapi.imgui.dsl

/**
 * Interface for components that support conditional display.
 * 
 * This interface provides a consistent way to conditionally show/hide
 * UI components based on boolean conditions.
 * 
 * @author Mark
 * @since 1.0.0
 */
interface ConditionalDisplay {
    /**
     * Conditionally displays the component based on a boolean condition.
     * 
     * @param condition The condition to check
     * @return This builder for method chaining
     */
    fun displayWhen(condition: Boolean): ConditionalDisplay
    
    /**
     * Conditionally displays the component based on a condition function.
     * 
     * @param condition The condition function to check
     * @return This builder for method chaining
     */
    fun displayWhen(condition: () -> Boolean): ConditionalDisplay
}
