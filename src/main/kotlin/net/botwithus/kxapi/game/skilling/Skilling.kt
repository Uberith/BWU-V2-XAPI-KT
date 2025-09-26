package net.botwithus.kxapi.game.skilling

import net.botwithus.kxapi.game.skilling.production.ProductionBuilder
import net.botwithus.kxapi.game.skilling.production.ProductionTypeSelector
import net.botwithus.xapi.script.BwuScript

/**
 * DSL context for skilling operations.
 * Provides a structured namespace for production-related operations.
 */
class Skilling(private val script: BwuScript) {
    
    /**
     * Production DSL for creating and configuring production tasks.
     * 
     * @param builder Lambda to configure the production task using ProductionBuilder DSL
     * @return Configured ProductionTypeSelector
     */
    fun production(builder: ProductionBuilder.() -> Unit): ProductionTypeSelector {
        return ProductionBuilder()
            .apply(builder)
            .build(script)
    }
    
    /**
     * Quick production task creation without DSL builder.
     * 
     * @param itemName The name of the item to produce
     * @param category The production category (e.g., "Cooking", "Smithing", "Crafting")
     * @return Configured ProductionTypeSelector
     */
    fun produce(itemName: String, category: String): ProductionTypeSelector {
        return ProductionBuilder()
            .itemName(itemName)
            .category(category)
            .build(script)
    }
    
    /**
     * Advanced production configuration with more control.
     * 
     * @param builder Lambda to configure the production task using ProductionBuilder DSL
     * @return Configured ProductionTypeSelector
     */
    fun advancedProduction(builder: ProductionBuilder.() -> Unit): ProductionTypeSelector {
        return ProductionBuilder()
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
