package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui groups with a fluent interface.
 * 
 * This class provides methods for grouping components together
 * and managing their layout.
 * 
 * @author Mark
 * @since 1.0.0
 */
class GroupBuilder {
    
    /**
     * Executes the content within a group context.
     * 
     * @param content The content to execute within the group
     */
    fun content(content: () -> Unit) {
        ImGui.beginGroup()
        content()
        ImGui.endGroup()
    }
}
