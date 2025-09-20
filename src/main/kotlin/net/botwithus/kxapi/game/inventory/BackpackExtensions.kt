package net.botwithus.kxapi.game.inventory

import net.botwithus.rs3.item.InventoryItem
import net.botwithus.xapi.game.inventory.Backpack
import net.botwithus.xapi.game.inventory.Backpack.getItem
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(Backpack::class.java)

object BackpackExtensions  {
    /**
     * Attempts to interact with [item] using the given [option].
     *
     * @return `true` if the interaction succeeded, `false` otherwise.
     */
    fun interact(item: InventoryItem, option: String): Boolean =
        runCatching {
            logger.info("[Backpack] Interact: option='{}', item='{}' ({})", option, item.name, item.id)
            val success = item.interact(option) > 0
            if (!success) {
                logger.warn("[Backpack] Interaction failed: option='{}', item='{}' ({})", option, item.name, item.id)
            }
            success
        }.getOrElse {
            logger.error("[Backpack] Exception during interact", it)
            false
        }

    /**
     * Finds the first item whose name matches any of [names] exactly and interacts with it using [option].
     */
    fun interact(option: String, vararg names: String): Boolean =
        getItem(*names)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by names failed: option='{}', names={}", option, names.toList())
            false
        }

    /**
     * Finds the first item whose name matches [spred] against any of [names],
     * then interacts with it using [option].
     */
    fun interact(
        spred: (String, CharSequence) -> Boolean,
        option: String,
        vararg names: String
    ): Boolean =
        getItem(spred, *names)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by predicate failed: option='{}', names={}", option, names.toList())
            false
        }

    /**
     * Finds the first item with any of [ids] and interacts with [option].
     */
    fun interact(option: String, vararg ids: Int): Boolean =
        getItem(*ids)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by ids failed: option='{}', ids={}", option, ids.toList())
            false
        }
}