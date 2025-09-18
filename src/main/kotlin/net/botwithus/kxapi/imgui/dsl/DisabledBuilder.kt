package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui disabled components with a fluent interface.
 * 
 * This class provides methods for managing disabled state of components.
 * 
 * @author Mark
 * @since 1.0.0
 */
class DisabledBuilder(private val disabled: Boolean) {
    
    /**
     * Executes the content within the disabled context.
     * 
     * @param content The content to execute within the disabled context
     */
    fun content(content: () -> Unit) {
        ImGui.beginDisabled(disabled)
        content()
        ImGui.endDisabled()
    }
}
