package net.botwithus.kxapi.game.skilling

import net.botwithus.kxapi.game.skilling.production.*
import net.botwithus.kxapi.game.skilling.production.ui.TypedProductionBuilder
import net.botwithus.kxapi.game.skilling.production.ui.impl.normal.NormalProduction
import net.botwithus.kxapi.game.skilling.production.ui.impl.smithing.SmeltingProduction
import net.botwithus.kxapi.game.skilling.production.ui.impl.smithing.SmithingProduction
import net.botwithus.xapi.script.BwuScript

/**
 * DSL context for skilling operations.
 * Provides a structured namespace for production-related operations.
 */
class Skilling(val script: BwuScript) {
    /**
     * Generic typed production DSL that works with any TypedProductionBuilder.
     * 
     * @param T The type of production builder to use
     * @param builder Lambda to configure the production task using the specified builder DSL
     * @return Configured ProductionManager
     */
    inline fun <reified T : TypedProductionBuilder<T>> productionOf(builder: T.() -> Unit): ProductionManager {
        @Suppress("UNCHECKED_CAST")
        val builderInstance = when (T::class) {
            NormalProduction::class -> NormalProduction() as T
            SmeltingProduction::class -> SmeltingProduction() as T
            SmithingProduction::class -> SmithingProduction() as T
            else -> throw IllegalArgumentException("Unsupported production builder type: ${T::class.simpleName}")
        }
        
        return builderInstance
            .apply(builder)
            .build(this.script)
    }

    /**
     * Typed production DSL for normal production activities (cooking, crafting, fletching, etc.).
     *
     * @param builder Lambda to configure the production task using NormalProductionBuilder DSL
     * @return Configured ProductionManager
     */
    fun production(builder: NormalProduction.() -> Unit): ProductionManager {
        return NormalProduction()
            .apply(builder)
            .build(script)
    }

    /**
     * Quick production task creation without DSL builder.
     *
     * @param itemName The name of the item to produce
     * @param category The production category (e.g., "Cooking", "Smithing", "Crafting")
     * @return Configured ProductionManager
     */
    fun produce(itemName: String, category: String): ProductionManager {
        return NormalProduction()
            .itemName(itemName)
            .category(category)
            .build(script)
    }
    
    /**
     * Advanced production configuration with more control.
     *
     * @param builder Lambda to configure the production task using NormalProductionBuilder DSL
     * @return Configured ProductionManager
     */
    fun advancedProduction(builder: NormalProduction.() -> Unit): ProductionManager {
        return NormalProduction()
            .apply(builder)
            .build(script)
    }
}

/**
 * Extension function on BwuScript to provide skilling DSL access.
 * 
 * @return SkillingDSL instance for method chaining
 */
val BwuScript.skilling: Skilling
    get() = Skilling(this)
