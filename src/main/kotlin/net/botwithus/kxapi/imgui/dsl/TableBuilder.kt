package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui tables with a fluent interface.
 * 
 * This class provides methods for configuring table properties and
 * managing table content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class TableBuilder(private val id: String, private val columns: Int) {
    private var flags = 0
    private var outerSizeX = 0f
    private var outerSizeY = 0f
    private var innerWidth = 0f
    
    /**
     * Sets the table flags.
     * 
     * @param flags Table flags
     * @return This builder for method chaining
     */
    fun flags(flags: Int) = apply {
        this.flags = flags
    }
    
    /**
     * Sets the outer size of the table.
     * 
     * @param x Outer width
     * @param y Outer height
     * @return This builder for method chaining
     */
    fun outerSize(x: Float, y: Float) = apply {
        this.outerSizeX = x
        this.outerSizeY = y
    }
    
    /**
     * Sets the inner width of the table.
     * 
     * @param width Inner width
     * @return This builder for method chaining
     */
    fun innerWidth(width: Float) = apply {
        this.innerWidth = width
    }
    
    /**
     * Executes the content within the table context.
     * 
     * @param content The content to execute within the table
     * @return True if the table was created successfully
     */
    fun content(content: () -> Unit): Boolean {
        val success = ImGui.beginTable(id, columns, flags, outerSizeX, outerSizeY, innerWidth)
        
        if (success) {
            content()
        }
        
        ImGui.endTable()
        return success
    }
}
