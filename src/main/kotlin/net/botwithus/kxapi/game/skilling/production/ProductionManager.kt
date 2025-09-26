package net.botwithus.kxapi.game.skilling.production

import net.botwithus.kxapi.util.componentIndex
import net.botwithus.kxapi.util.findByContainsText
import net.botwithus.kxapi.game.skilling.production.ui.ProductionInterfaceBase
import net.botwithus.rs3.interfaces.Interfaces
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.script.BwuScript

private const val PRODUCTION_INTERFACE = 1251

/**
 * Handles production type selection and item production in RuneScape 3.
 * Supports both Make-X interface and production interfaces.
 */
class ProductionManager(
    private val script: BwuScript,
    private val interfaceHandler: ProductionInterfaceBase
) {

    private var firstRun = true
    private var lastProgressString: String? = null
    private var xp: Double = 0.0

    /**
     * Main method to handle item production flow.
     * @param onFinished Callback when production is complete
     * @param onProgress Callback for progress updates
     */
    fun produceItem(
        onFinished: (Double) -> Unit = { _ -> },
        onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
    ) {
        when {
            Interfaces.isOpen(PRODUCTION_INTERFACE) -> {
                handleProductionInterface(onProgress)
            }
            Interfaces.isOpen(interfaceHandler.makeXInterface()) -> {
                val result = interfaceHandler.handle()
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
        interfaceHandler.clearCacheIfNeeded()
    }
    
    /**
     * Check if the item can be produced using the interface handler.
     * 
     * @return true if the item can be produced, false otherwise
     */
    fun canProduce(): Boolean {
        return interfaceHandler.canProduce()
    }

    /**
     * Handle production interface updates and progress tracking.
     */
    private fun handleProductionInterface(onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit) {
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
        onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit
    ) {
        if (progress.size == 2) {
            val progressString = "${progress[0]}/${progress[1]}"
            if (progressString != lastProgressString) {
                val currentProgress = progress[0].toIntOrNull() ?: 0
                val totalProgress = progress[1].toIntOrNull() ?: 0
                val itemsPerSecond = status.replace("s", "").toIntOrNull() ?: 0

                onProgress(
                    ProductionResult.CREATION_IN_PROGRESS.toMessage(),
                    currentProgress,
                    totalProgress,
                    itemsPerSecond,
                    xpGained
                )
                lastProgressString = progressString
            }
        }
    }

}
