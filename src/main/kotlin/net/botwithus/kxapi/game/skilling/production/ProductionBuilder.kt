package net.botwithus.kxapi.game.skilling.production

import net.botwithus.xapi.script.BwuScript

/**
 * Builder class for creating ProductionTypeSelector instances.
 * Supports fluent DSL syntax for configuring production tasks.
 */
class ProductionBuilder {
    private lateinit var _itemName: String
    private lateinit var _category: String

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
     * Alternative method name for itemName for more natural DSL syntax.
     * 
     * @param name The item name
     * @return This builder instance for method chaining
     */
    fun item(name: String) = itemName(name)
    
    /**
     * Alternative method name for category for more natural DSL syntax.
     * 
     * @param name The category name
     * @return This builder instance for method chaining
     */
    fun of(name: String) = category(name)

    /**
     * Build the ProductionTypeSelector with the configured parameters.
     * 
     * @param script The SuspendableScript instance to use
     * @return Configured ProductionTypeSelector
     * @throws IllegalStateException if required parameters are not set
     */
    fun build(script: BwuScript): ProductionTypeSelector {
        require(::_itemName.isInitialized) { "Item name must be set" }
        require(::_category.isInitialized) { "Production category must be set" }
        return ProductionTypeSelector(script, _itemName, _category)
    }
}