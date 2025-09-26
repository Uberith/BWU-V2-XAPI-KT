package net.botwithus.kxapi.game.skilling.production.ui.impl.smithing

import net.botwithus.kxapi.game.skilling.production.ProductionManager
import net.botwithus.kxapi.game.skilling.production.ui.TypedProductionBuilder
import net.botwithus.xapi.script.BwuScript

/**
 * Builder for smelting production activities in RuneScape 3.
 * Handles the creation of metal bars from ores.
 */
class SmeltingProduction : TypedProductionBuilder<SmeltingProduction>() {

    lateinit var _inputName: String
    lateinit var _outputName: String

    /**
     * Public read-only getters (hide underscores).
     */
    val inputName: String
        get() = _inputName

    val outputName: String
        get() = _outputName

    /**
     * Set the name of the input ore to smelt.
     *
     * @param name The ore name (e.g., "Iron ore", "Gold ore")
     * @return This builder instance for method chaining
     */
    fun input(name: String) = apply { this._inputName = name }

    /**
     * Set the name of the output bar to produce.
     *
     * @param name The bar name (e.g., "Iron bar", "Gold bar")
     * @return This builder instance for method chaining
     */
    fun output(name: String) = apply { this._outputName = name }

    /**
     * Build the ProductionManager for smelting production.
     */
    override fun build(script: BwuScript): ProductionManager {
        require(::_inputName.isInitialized) { "Input ore name must be set" }
        require(::_outputName.isInitialized) { "Output bar name must be set" }
        val interfaceHandler = MetalInterface.forSmelting(script, this)
        return ProductionManager(script, interfaceHandler)
    }

}