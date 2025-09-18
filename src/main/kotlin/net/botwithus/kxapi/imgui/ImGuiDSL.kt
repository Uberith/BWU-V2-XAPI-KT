package net.botwithus.kxapi.imgui

import net.botwithus.kxapi.imgui.dsl.ButtonBuilder
import net.botwithus.kxapi.imgui.dsl.ChildWindowBuilder
import net.botwithus.kxapi.imgui.dsl.DisabledBuilder
import net.botwithus.kxapi.imgui.dsl.GroupBuilder
import net.botwithus.kxapi.imgui.dsl.LayoutBuilder
import net.botwithus.kxapi.imgui.dsl.MenuBuilder
import net.botwithus.kxapi.imgui.dsl.PopupBuilder
import net.botwithus.kxapi.imgui.dsl.SectionBuilder
import net.botwithus.kxapi.imgui.dsl.StyleBuilder
import net.botwithus.kxapi.imgui.dsl.TabBarBuilder
import net.botwithus.kxapi.imgui.dsl.TableBuilder
import net.botwithus.kxapi.imgui.dsl.TextBuilder
import net.botwithus.kxapi.imgui.dsl.TooltipBuilder
import net.botwithus.kxapi.imgui.dsl.TreeNodeBuilder
import net.botwithus.kxapi.imgui.dsl.WindowBuilder

/**
 * Main DSL entry point for ImGui operations.
 * 
 * This class provides a fluent, type-safe interface for building ImGui UIs
 * with a clean, readable syntax. It handles the creation and management of
 * all ImGui components and their styling.
 * 
 * @author Mark
 * @since 1.0.0
 */
object ImGuiDSL {
    
    /**
     * Creates a new window with the specified name.
     * 
     * @param name The window name
     * @return A WindowBuilder for configuring the window
     */
    fun window(name: String) = WindowBuilder(name, 0)
    
    /**
     * Creates a new window with the specified name and flags.
     * 
     * @param name The window name
     * @param flags Window flags (use ImGuiWindowFlags constants)
     * @return A WindowBuilder for configuring the window
     */
    fun window(name: String, flags: Int) = WindowBuilder(name, flags)
    
    /**
     * Creates a new section (collapsing header) with the specified name.
     * 
     * @param name The section name
     * @return A SectionBuilder for configuring the section
     */
    fun section(name: String) = SectionBuilder(name, 0)
    
    /**
     * Creates a new section (collapsing header) with the specified name and flags.
     * 
     * @param name The section name
     * @param flags Section flags
     * @return A SectionBuilder for configuring the section
     */
    fun section(name: String, flags: Int) = SectionBuilder(name, flags)
    
    /**
     * Creates a text component builder.
     * 
     * @return A TextBuilder for configuring text display
     */
    fun text() = TextBuilder()
    
    /**
     * Creates a text component with the specified text and DSL block.
     * 
     * @param text The text content
     * @param block Configuration block
     */
    fun text(text: String, block: TextBuilder.() -> Unit) = TextBuilder().invoke(text, block)
    
    /**
     * Creates a button component builder.
     * 
     * @param label The button label
     * @return A ButtonBuilder for configuring the button
     */
    fun button(label: String) = ButtonBuilder(label)
    
    /**
     * Creates a group component builder.
     * 
     * @return A GroupBuilder for grouping components
     */
    fun group() = GroupBuilder()
    
    /**
     * Creates a table component builder.
     * 
     * @param id The table ID
     * @param columns Number of columns
     * @return A TableBuilder for configuring the table
     */
    fun table(id: String, columns: Int) = TableBuilder(id, columns)
    
    /**
     * Creates a tab bar component builder.
     * 
     * @param id The tab bar ID
     * @return A TabBarBuilder for configuring the tab bar
     */
    fun tabBar(id: String) = TabBarBuilder(id)
    
    /**
     * Creates a popup component builder.
     * 
     * @param id The popup ID
     * @return A PopupBuilder for configuring the popup
     */
    fun popup(id: String) = PopupBuilder(id)
    
    /**
     * Creates a tooltip component builder.
     * 
     * @return A TooltipBuilder for configuring the tooltip
     */
    fun tooltip() = TooltipBuilder()
    
    /**
     * Creates a menu component builder.
     * 
     * @param label The menu label
     * @return A MenuBuilder for configuring the menu
     */
    fun menu(label: String) = MenuBuilder(label)
    
    /**
     * Creates a tree node component builder.
     * 
     * @param label The tree node label
     * @return A TreeNodeBuilder for configuring the tree node
     */
    fun treeNode(label: String) = TreeNodeBuilder(label)
    
    /**
     * Creates a child window component builder.
     * 
     * @param id The child window ID
     * @return A ChildWindowBuilder for configuring the child window
     */
    fun child(id: String) = ChildWindowBuilder(id)
    
    /**
     * Creates a disabled component builder.
     * 
     * @param disabled Whether the component should be disabled
     * @return A DisabledBuilder for configuring disabled state
     */
    fun disabled(disabled: Boolean) = DisabledBuilder(disabled)
    
    /**
     * Creates a style component builder for managing ImGui styles.
     * 
     * @return A StyleBuilder for configuring ImGui styles
     */
    fun style() = StyleBuilder()
    
    /**
     * Creates a layout component builder for managing positioning and spacing.
     * 
     * @return A LayoutBuilder for configuring layout
     */
    fun layout() = LayoutBuilder()
}

/**
 * Extension function for easy access to ImGui DSL
 */
fun imgui(block: ImGuiDSL.() -> Unit) = ImGuiDSL.block()
