package net.botwithus.kxapi.imgui

import net.botwithus.xapi.script.ui.interfaces.BuildableUI

/**
 * A wrapper for BuildableUI that automatically handles the imgui context.
 *
 * This allows you to focus on the content without worrying about the imgui wrapper.
 * Simply implement the build() method with your UI content using imguiUI { }.
 *
 * @author Mark
 * @since 1.0.0
 */
abstract class ImGuiUI : BuildableUI {

    /**
     * Builds the UI content within an imgui context.
     *
     * Override this method to return imguiUI { } with your UI content.
     */
    abstract fun build(): ImGuiDSL.() -> Unit

    /**
     * Implementation of BuildableUI.buildUI() that wraps the build() method in imgui context.
     */
    override fun buildUI() {
        imgui {
            build()(ImGuiDSL)
        }
    }

    /**
     * Provides access to the ImGui DSL functions within the build() method.
     * Use this to access all ImGui DSL functions without individual imports.
     */
    fun imguiUI(block: ImGuiDSL.() -> Unit): ImGuiDSL.() -> Unit = block
}