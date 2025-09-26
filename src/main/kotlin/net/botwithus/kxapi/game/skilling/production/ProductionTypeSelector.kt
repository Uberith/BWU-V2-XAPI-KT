package net.botwithus.kxapi.game.skilling.production

/**
 * Backwards-compatible wrapper around [ProductionManager] that retains the legacy
 * `ProductionTypeSelector` surface expected by existing scripts.
 */
class ProductionTypeSelector internal constructor(
    private val manager: ProductionManager
) {

    /** Access to the underlying manager for advanced use cases. */
    val delegate: ProductionManager
        get() = manager

    fun produceItem(
        onFinished: (Double) -> Unit = { _ -> },
        onProgress: (ProductionResult, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
    ) {
        manager.produceItem(onFinished) { message, current, total, rate, xp ->
            onProgress(message.result, current, total, rate, xp)
        }
    }

    fun canProduce(): Boolean = manager.canProduce()
}

internal fun ProductionManager.asTypeSelector(): ProductionTypeSelector = ProductionTypeSelector(this)
