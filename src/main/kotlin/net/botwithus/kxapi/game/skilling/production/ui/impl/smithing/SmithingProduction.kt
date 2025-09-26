package net.botwithus.kxapi.game.skilling.production.ui.impl.smithing

import net.botwithus.kxapi.game.skilling.production.ProductionManager
import net.botwithus.kxapi.game.skilling.production.ui.TypedProductionBuilder
import net.botwithus.xapi.script.BwuScript

/**
 * Builder for smithing production activities in RuneScape 3.
 * Handles the creation of weapons, armor, and tools from metal bars.
 */

/**
 * Represents the different base modifiers available for smithing items in RuneScape 3.
 * These correspond to the different enhancement levels (base, +1, +2, etc.) and burial variants.
 */
enum class SmithingBaseModifier(val display: String, val componentID: Int) {
    BASE("base", 149),
    PLUS_1("+ 1", 161),
    PLUS_2("+ 2", 159),
    PLUS_3("+ 3", 157),
    PLUS_4("+ 4", 155),
    PLUS_5("+ 5", 153),
    BURIAL("burial", 151)
}

class SmithingProduction : TypedProductionBuilder<SmithingProduction>() {

    lateinit var _inputName: String
    lateinit var _outputName: String
    var _modifier: SmithingBaseModifier = SmithingBaseModifier.BASE

    /**
     * Public read-only getters (hide underscores).
     */
    val inputName: String
        get() = _inputName

    val outputName: String
        get() = _outputName

    val modifier: SmithingBaseModifier
        get() = _modifier


    /**
     * Set the name of the input bar to smith with.
     *
     * @param name The bar name (e.g., "Iron bar", "Steel bar")
     * @return This builder instance for method chaining
     */
    fun input(name: String) = apply { this._inputName = name }

    /**
     * Set the name of the output item to produce.
     *
     * @param name The item name (e.g., "Iron dagger", "Steel sword")
     * @return This builder instance for method chaining
     */
    fun output(name: String) = apply { this._outputName = name }

    /**
     * Sets the base item mode for this builder.
     *
     * In RS3 this corresponds to the "base", "base+1", "base+2", etc. variants
     * that determine which item mode is used.
     *
     * @param name The base item mode name (e.g., "base", "base+1", "base+2")
     * @return This builder instance for method chaining
     */
    fun modifier(modifier: SmithingBaseModifier) = apply { this._modifier = modifier }

    /**
     * Build the ProductionManager for smithing production.
     */
    override fun build(script: BwuScript): ProductionManager {
        require(::_inputName.isInitialized) { "Input bar name must be set" }
        require(::_outputName.isInitialized) { "Output item name must be set" }
        val interfaceHandler = MetalInterface.forSmithing(script, this)
        return ProductionManager(script, interfaceHandler)
    }

}