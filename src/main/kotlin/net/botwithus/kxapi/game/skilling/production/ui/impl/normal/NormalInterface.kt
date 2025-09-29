package net.botwithus.kxapi.game.skilling.production.ui.impl.normal

import net.botwithus.kxapi.game.skilling.production.ProductionResult
import net.botwithus.kxapi.game.skilling.production.ProductionMessage
import net.botwithus.kxapi.game.skilling.production.ui.ProductionInterfaceBase
import net.botwithus.kxapi.game.skilling.production.toMessage
import net.botwithus.kxapi.util.componentIndex
import net.botwithus.kxapi.util.findBySubComponentId
import net.botwithus.kxapi.util.findByText
import net.botwithus.kxapi.util.findChild
import net.botwithus.kxapi.util.findItemByName
import net.botwithus.kxapi.util.interactWithOption
import net.botwithus.kxapi.util.subComponentIndex
import net.botwithus.rs3.cs2.Layout
import net.botwithus.rs3.cs2.ScriptDescriptor
import net.botwithus.rs3.cs2.ScriptHandle
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.script.BwuScript

class NormalInterface(
    private val script : BwuScript,
    private val settings: NormalProduction
) : ProductionInterfaceBase() {

    private val MAKE_X_INTERFACE = 1370
    private val PRODUCTION_MENU_INTERFACE = 1371
    private val MENU_INTERFACE = 1477

    override fun handle(): ProductionMessage<ProductionResult>? {
        val selectedItemText = ComponentQuery.newQuery(MAKE_X_INTERFACE)
            .componentIndex(13)
            .firstOrNull()
            ?.text
            ?.replace("<br>", "")
            ?.replace(Regex("\\s*(x?\\d+)$"), "")
        ?: return ProductionResult.INTERFACE_ERROR.toMessage()

        return if (selectedItemText == settings.itemName) {
            if (canProduce()) {
                val descriptor = ScriptDescriptor.of(emptyList<Layout>(), Layout.INTEGER)
                val handle = ScriptHandle.of(6970, descriptor) ?: error("Script not found")
                val result = handle.invokeExact(83)

                ProductionResult.CREATION_IN_PROGRESS.toMessage()
            } else {
                ProductionResult.MISSING_REQUIREMENTS.toMessage()
            }
        } else {
            selectProductionType()
        }
    }

    override fun makeXInterface() = MAKE_X_INTERFACE

    private fun selectProductionType(): ProductionMessage<ProductionResult>? {
        val currentType = ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE)
            .componentIndex(27).subComponentIndex(3)?.text
            ?: return ProductionResult.INTERFACE_ERROR.toMessage()

        if (currentType != settings.category) {
            clearCacheIfNeeded()
            return changeProductionType()
        } else {
            return selectItem()
        }
    }

    private fun changeProductionType(): ProductionMessage<ProductionResult>? {
        val menuComponent = ComponentQuery.newQuery(MENU_INTERFACE).componentIndex(910).results().firstOrNull()
            ?: return ProductionResult.INTERFACE_ERROR.toMessage()

        val menuHidden = menuComponent.isHidden

        if (!menuHidden) {
            val menuItem = cachedMenuItem ?: findAndCacheMenuItem()
            if (menuItem.first == 0 && menuItem.second == 0) {
                return ProductionResult.Normal.CATEGORY_NOT_FOUND.toMessage()
            }
            return selectMenuOption(menuItem)
        } else {
            openProductionTypeMenu()
            script.delay(3)
            return null
        }
    }

    private fun findAndCacheMenuItem(): Pair<Int, Int> {
        val menuItem = ComponentQuery.newQuery(MENU_INTERFACE)
            .findByText(settings.category)
            ?.let { it.componentId to it.subComponentId } ?: (0 to 0)

        cachedMenuItem = menuItem
        return menuItem
    }

    private fun selectMenuOption(menuItem: Pair<Int, Int>): ProductionMessage<ProductionResult>? {
        val success = ComponentQuery.newQuery(MENU_INTERFACE).componentIndex(menuItem.first + 1)
            .findBySubComponentId((menuItem.second * 2) + 1)?.interactWithOption("Select") ?: false

        return if (success) null else ProductionResult.Normal.CATEGORY_NOT_FOUND.toMessage()
    }

    private fun openProductionTypeMenu() {
        ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE).componentIndex(28).first().interact()
    }

    private fun selectItem(): ProductionMessage<ProductionResult>? {
        val success = ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE)
            .componentIndex(22)
            .selectItemByName(settings.itemName) { itemId -> getItemName(itemId) }

        return if (success) null else ProductionResult.Normal.ITEM_NOT_FOUND.toMessage()
    }


    override fun canProduce(): Boolean {
        return (ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE)
            .findChild { it.componentId == 19 && it.subComponentId == 4 }
            ?.text?.toIntOrNull() ?: 0) > 0
    }

    private fun ComponentQuery.selectItemByName(
        itemName: String,
        itemNameProvider: (Int) -> String
    ): Boolean {
        val targetChild = findItemByName(itemName, itemNameProvider)
        if (targetChild != null) {
            val buttonId = targetChild.subComponentId - 1
            return findBySubComponentId(buttonId)?.interactWithOption("Select") == true
        }
        return false
    }


}