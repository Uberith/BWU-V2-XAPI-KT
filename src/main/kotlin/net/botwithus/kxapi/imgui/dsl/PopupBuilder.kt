package net.botwithus.kxapi.imgui.dsl

import net.botwithus.imgui.ImGui

/**
 * Builder for creating ImGui popups with a fluent interface.
 * 
 * This class provides methods for configuring popup properties and
 * managing popup content.
 * 
 * @author Mark
 * @since 1.0.0
 */
class PopupBuilder(private val id: String) {
    private var flags = 0
    private var modal = false
    private var open = true
    
    /**
     * Sets the popup flags.
     * 
     * @param flags Popup flags
     * @return This builder for method chaining
     */
    fun flags(flags: Int) = apply {
        this.flags = flags
    }
    
    /**
     * Makes the popup modal.
     * 
     * @return This builder for method chaining
     */
    fun modal() = apply {
        this.modal = true
    }
    
    /**
     * Sets whether the popup is open.
     * 
     * @param open True if open
     * @return This builder for method chaining
     */
    fun open(open: Boolean) = apply {
        this.open = open
    }
    
    /**
     * Executes the content within the popup context.
     * 
     * @param content The content to execute within the popup
     * @return True if the popup is open
     */
    fun content(content: () -> Unit): Boolean {
        val isOpen = if (modal) {
            ImGui.beginPopupModal(id, open, flags)
        } else {
            ImGui.beginPopup(id, flags)
        }
        
        if (isOpen) {
            content()
        }
        
        ImGui.endPopup()
        return isOpen
    }
    
    /**
     * Opens the popup.
     */
    fun open() {
        ImGui.openPopup(id)
    }
    
    companion object {
        /**
         * Closes the current popup.
         */
        fun close() {
            ImGui.closeCurrentPopup()
        }
    }
}
