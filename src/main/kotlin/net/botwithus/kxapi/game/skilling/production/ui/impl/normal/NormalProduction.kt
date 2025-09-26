package net.botwithus.kxapi.game.skilling.production.ui.impl.normal

import net.botwithus.kxapi.game.skilling.production.ProductionManager
import net.botwithus.kxapi.game.skilling.production.ui.TypedProductionBuilder
import net.botwithus.xapi.script.BwuScript

/**
 * Builder for normal production activities (cooking, crafting, fletching, etc.)
 */
class NormalProduction : TypedProductionBuilder<NormalProduction>() {

    lateinit var _itemName: String
    lateinit var _category: String

    /**
     * Public read-only getters (hide underscores).
     */
    val itemName: String
        get() = _itemName

    val category: String
        get() = _category

    /**
     * Set the name of the item to produce.
     *
     * @param name The item name
     * @return This builder instance for method chaining
     */
    fun itemName(name: String) = apply { this._itemName = name }

    /**
     * Set the production category.
     *
     * @param name The category name (e.g., "Cooking", "Smithing", "Crafting")
     * @return This builder instance for method chaining
     */
    fun category(name: String) = apply { this._category = name }

    /**
     * Build the ProductionManager for normal production.
     */
    override fun build(script: BwuScript): ProductionManager {
        check(::_itemName.isInitialized) { "itemName must be set before building" }
        check(::_category.isInitialized) { "category must be set before building" }
        val interfaceHandler = NormalInterface(script, this)
        return ProductionManager(script, interfaceHandler)
    }
}

