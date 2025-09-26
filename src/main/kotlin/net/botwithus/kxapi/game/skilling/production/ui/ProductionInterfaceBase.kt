package net.botwithus.kxapi.game.skilling.production.ui

import net.botwithus.kxapi.game.skilling.production.ProductionResult
import net.botwithus.kxapi.game.skilling.production.ProductionMessage
import net.botwithus.rs3.cache.assets.ConfigManager

abstract class ProductionInterfaceBase {

    val itemNameCache = mutableMapOf<Int, String>()
    var cachedMenuItem: Pair<Int, Int>? = null

    fun clearCacheIfNeeded() {
        itemNameCache.clear()
        cachedMenuItem = null
    }

    fun getItemName(itemId: Int) = itemNameCache.getOrPut(itemId) {
        if (itemId != -1) ConfigManager.getItemProvider().provide(itemId).name else ""
    }

    abstract fun handle() : ProductionMessage<ProductionResult>?
    abstract fun makeXInterface() : Int
    abstract fun canProduce() : Boolean
}