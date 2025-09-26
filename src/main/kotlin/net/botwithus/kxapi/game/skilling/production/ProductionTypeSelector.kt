package net.botwithus.kxapi.game.skilling.production

import net.botwithus.kxapi.util.componentIndex
import net.botwithus.kxapi.util.findByContainsText
import net.botwithus.kxapi.util.findBySubComponentId
import net.botwithus.kxapi.util.findByText
import net.botwithus.kxapi.util.findChild
import net.botwithus.kxapi.util.interactWithOption
import net.botwithus.kxapi.util.isVisible
import net.botwithus.kxapi.util.selectItemByName
import net.botwithus.kxapi.util.subComponentIndex
import net.botwithus.rs3.cache.assets.ConfigManager
import net.botwithus.rs3.cs2.Layout
import net.botwithus.rs3.cs2.ScriptDescriptor
import net.botwithus.rs3.cs2.ScriptHandle
import net.botwithus.rs3.interfaces.Interfaces
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.script.BwuScript

private const val MAKE_X_INTERFACE = 1370
private const val PRODUCTION_INTERFACE = 1251
private const val MENU_INTERFACE = 1477
private const val PRODUCTION_MENU_INTERFACE = 1371

/**
 * Handles production type selection and item production in RuneScape 3.
 * Supports both Make-X interface and production interfaces.
 */
class ProductionTypeSelector(
    private val script: BwuScript,
    private val itemName: String,
    private val category: String
) {

    private var firstRun = true
    private var lastProgressString: String? = null
    private var xp: Double = 0.0
    

    private val itemNameCache = mutableMapOf<Int, String>()
    private var cachedMenuItem: Pair<Int, Int>? = null

    /**
     * Main method to handle item production flow.
     * @param onFinished Callback when production is complete
     * @param onProgress Callback for progress updates
     */
    fun produceItem(
        onFinished: (Double) -> Unit = { _ -> },
        onProgress: (ProductionResult, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
    ) {
        when {
            Interfaces.isOpen(PRODUCTION_INTERFACE) -> {
                handleProductionInterface(onProgress)
            }
            Interfaces.isOpen(MAKE_X_INTERFACE) -> {
                val result = handleMakeXInterface()
                result?.let { error ->
                    onProgress(error, 0, 0, 0, 0.0)
                    return
                }
            }
            lastProgressString != null -> {
                onFinished(xp)
                resetState()
            }
        }
    }

    /**
     * Reset internal state after production completion.
     */
    private fun resetState() {
        lastProgressString = null
        firstRun = true
        xp = 0.0
        clearCacheIfNeeded()
    }

    /**
     * Handle production interface updates and progress tracking.
     */
    private fun handleProductionInterface(onProgress: (ProductionResult, Int, Int, Int, Double) -> Unit) {
        val status = getProductionStatus()
        if (status.isEmpty()) return

        val progress = getProductionProgress()
        val xpGained = getXpGained()

        if (firstRun) {
            firstRun = false
            return
        }

        if (isProductionComplete(status, progress)) {
            this.xp = xpGained
            return
        }

        updateProgressIfChanged(progress, status, xpGained, onProgress)
    }

    private fun getProductionStatus(): String {
        return ComponentQuery.newQuery(PRODUCTION_INTERFACE)
            .componentIndex(10)
            .firstOrNull()
            ?.text
            .orEmpty()
    }

    private fun getProductionProgress(): List<String> {
        return ComponentQuery.newQuery(PRODUCTION_INTERFACE)
            .findByContainsText("/")
            ?.text
            ?.split("/")
            ?: emptyList()
    }

    private fun getXpGained(): Double {
        return ComponentQuery.newQuery(PRODUCTION_INTERFACE)
            .componentIndex(17)
            .first()
            .text
            .orEmpty()
            .replace("xp", "")
            .replace("+ ", "")
            .toDoubleOrNull() ?: 0.0
    }

    private fun isProductionComplete(status: String, progress: List<String>): Boolean {
        return status == "Done" && 
               progress.size == 2 && 
               progress[0] == progress[1]
    }

    private fun updateProgressIfChanged(
        progress: List<String>,
        status: String,
        xpGained: Double,
        onProgress: (ProductionResult, Int, Int, Int, Double) -> Unit
    ) {
        if (progress.size == 2) {
            val progressString = "${progress[0]}/${progress[1]}"
            if (progressString != lastProgressString) {
                val currentProgress = progress[0].toIntOrNull() ?: 0
                val totalProgress = progress[1].toIntOrNull() ?: 0
                val itemsPerSecond = status.replace("s", "").toIntOrNull() ?: 0
                
                onProgress(
                    ProductionResult.CREATION_IN_PROGRESS,
                    currentProgress,
                    totalProgress,
                    itemsPerSecond,
                    xpGained
                )
                lastProgressString = progressString
            }
        }
    }

    private fun handleMakeXInterface(): ProductionResult? {
        val selectedItemText = ComponentQuery.newQuery(MAKE_X_INTERFACE)
            .componentIndex(13)
            .isVisible()
            .results()
            .firstOrNull()
            ?.text
            ?.replace("<br>", "")
            ?.replace(Regex("\\s*(x?\\d+)$"), "")
            ?: return ProductionResult.INTERFACE_ERROR

        if (selectedItemText == itemName) {
            return if (canProduce()) {
                val descriptor = ScriptDescriptor.of(emptyList<Layout>(), Layout.INTEGER)
                val handle = ScriptHandle.of(6970, descriptor) ?: error("Script not found")
                val result = handle.invokeExact(83)

                ProductionResult.CREATION_IN_PROGRESS
            } else {
                ProductionResult.MISSING_REQUIREMENTS
            }
        } else {
            return selectProductionType()
        }
    }

    fun canProduce(): Boolean {
        return (ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE).findChild {
            it.componentId == 19 && it.subComponentId == 4 }
            ?.text?.toIntOrNull() ?: 0) > 0
    }

    private fun selectProductionType(): ProductionResult? {
        val currentType = ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE)
            .componentIndex(27).subComponentIndex(3)?.text
            ?: return ProductionResult.INTERFACE_ERROR

        if (currentType != category) {
            clearCacheIfNeeded()
            return changeProductionType()
        } else {
            return selectItem()
        }
    }

    private fun changeProductionType(): ProductionResult? {
        val menuComponent = ComponentQuery.newQuery(MENU_INTERFACE).componentIndex(910).results().firstOrNull()
            ?: return ProductionResult.INTERFACE_ERROR
        
        val menuHidden = menuComponent.isHidden

        if (!menuHidden) {
            val menuItem = cachedMenuItem ?: findAndCacheMenuItem()
            if (menuItem.first == 0 && menuItem.second == 0) {
                return ProductionResult.CATEGORY_NOT_FOUND
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
            .findByText(category)
            ?.let { it.componentId to it.subComponentId } ?: (0 to 0)

        cachedMenuItem = menuItem
        return menuItem
    }

    private fun selectMenuOption(menuItem: Pair<Int, Int>): ProductionResult? {
        val success = ComponentQuery.newQuery(MENU_INTERFACE).componentIndex(menuItem.first + 1)
            .findBySubComponentId((menuItem.second * 2) + 1)?.interactWithOption("Select") ?: false
        
        return if (success) null else ProductionResult.CATEGORY_NOT_FOUND
    }

    private fun openProductionTypeMenu() {
        ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE).componentIndex(28).first().interact()
    }

    private fun selectItem(): ProductionResult? {
        val success = ComponentQuery.newQuery(PRODUCTION_MENU_INTERFACE).componentIndex(22).selectItemByName(itemName) {
            itemId -> getItemName(itemId)
        }
        
        return if (success) null else ProductionResult.ITEM_NOT_FOUND
    }

    private fun getItemName(itemId: Int) = itemNameCache.getOrPut(itemId) {
        if (itemId != -1) ConfigManager.getItemProvider().provide(itemId).name else ""
    }

    private fun clearCacheIfNeeded() {
        itemNameCache.clear()
        cachedMenuItem = null
    }

}
