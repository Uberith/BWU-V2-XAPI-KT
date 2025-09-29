package net.botwithus.kxapi.game.skilling.production.ui

import net.botwithus.kxapi.game.skilling.production.ProductionManager
import net.botwithus.xapi.script.BwuScript

/**
 * Base sealed class for typed production builders.
 * Each production type can have its own specialized builder with type-specific methods.
 */
abstract class TypedProductionBuilder<T : TypedProductionBuilder<T>> {
    /**
     * Build the ProductionTypeSelector with the configured parameters.
     * Must be implemented by each typed builder.
     */
    abstract fun build(script: BwuScript): ProductionManager

}