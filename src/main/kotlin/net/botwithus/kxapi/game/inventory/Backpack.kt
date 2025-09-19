package net.botwithus.kxapi.game.inventory


import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.inventories.InventoryManager
import net.botwithus.rs3.item.InventoryItem
import java.util.regex.Pattern

/**
 * Lightweight Kotlin helpers for the player backpack (inventory id 93).
 */
object Backpack {

    private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(Backpack::class.java)
    private const val INVENTORY_ID = 93

    /** Returns the backpack inventory, or null if unavailable. */
    fun getInventory(): Inventory? = InventoryManager.getInventory(INVENTORY_ID)

    /** Returns true when all slots are occupied by non-empty items. */
    fun isFull(): Boolean {
        val inv = getInventory() ?: return false
        val filled = inv.items.count { it.id != -1 }
        val cap = inv.definition.capacity
        val full = filled == cap
        logger.info("[Backpack] isFull? {}/{} -> {}", filled, cap, full)
        return full
    }

    /** Returns true when all slots are empty. */
    fun isEmpty(): Boolean {
        val inv = getInventory() ?: return true
        val empty = inv.items.all { it.id == -1 }
        logger.info("[Backpack] isEmpty? -> {}", empty)
        return empty
    }

    /** Returns all non-empty items in the backpack. */
    fun getItems(): List<InventoryItem> =
        (getInventory()?.items?.filter { it.name.isNotEmpty() } ?: emptyList()).also {
            logger.info("[Backpack] getItems() -> {} items", it.size)
        }

    /**
     * Returns true if any item name matches any of [names] using [spred].
     * Predicate receives (needle, haystack) -> Boolean.
     */
    fun contains(spred: (String, CharSequence) -> Boolean, vararg names: String): Boolean {
        if (names.isEmpty()) return false
        val inv = getInventory() ?: return false
        val query = names.asList()
        val result = inv.items.any { item ->
            val nm = item.name
            nm.isNotEmpty() && query.any { q -> spred(q, nm) }
        }
        logger.info("[Backpack] contains(names={}, pred) -> {}", query, result)
        return result
    }

    /** Returns true if any item name equals any of [names] (contentEquals semantics). */
    fun contains(vararg names: String): Boolean =
        contains({ a, b -> a.contentEquals(b) }, *names)

    /** Returns true if any item id matches any of [ids]. */
    fun contains(vararg ids: Int): Boolean {
        if (ids.isEmpty()) return false
        val set = ids.toSet()
        val inv = getInventory() ?: return false
        val result = inv.items.any { it.id in set }
        logger.info("[Backpack] contains(ids={}) -> {}", set, result)
        return result
    }

    /** Returns true if any item name matches any of the [namePatterns] (full match). */
    fun contains(vararg namePatterns: Pattern): Boolean {
        if (namePatterns.isEmpty()) return false
        val inv = getInventory() ?: return false
        val result = inv.items.any { item ->
            val nm = item.name
            nm.isNotEmpty() && namePatterns.any { p -> p.matcher(nm).matches() }
        }
        logger.info("[Backpack] contains(patterns x{}) -> {}", namePatterns.size, result)
        return result
    }

    /** Returns the first item whose name matches [spred] against any of [names]. */
    fun getItem(spred: (String, CharSequence) -> Boolean, vararg names: String): InventoryItem? {
        if (names.isEmpty()) return null
        val inv = getInventory() ?: return null
        val found = inv.items.firstOrNull { item ->
            val nm = item.name
            nm.isNotEmpty() && names.any { q -> spred(q, nm) }
        }
        logger.info("[Backpack] getItem(names={}) -> {}", names.asList(), found?.let { "${it.name} (${it.id})" } ?: "null")
        return found
    }

    /** Returns the first item whose name equals any of [names] (contentEquals semantics). */
    fun getItem(vararg names: String): InventoryItem? =
        getItem({ a, b -> a.contentEquals(b) }, *names)

    /** Returns the first item whose id is in [ids]. */
    fun getItem(vararg ids: Int): InventoryItem? {
        if (ids.isEmpty()) return null
        val set = ids.toSet()
        val inv = getInventory() ?: return null
        val found = inv.items.firstOrNull { it.id in set }
        logger.info("[Backpack] getItem(ids={}) -> {}", set, found?.let { "${it.name} (${it.id})" } ?: "null")
        return found
    }

    /** Interacts with the provided [item] using the given [option]. */
    fun interact(item: InventoryItem, option: String): Boolean =
        try {
            logger.info("[Backpack] Interact: option='{}', item='{}' ({})", option, item.name, item.id)
            val ok = item.interact(option) > 0
            if (!ok) logger.warn("[Backpack] Interaction failed: option='{}', item='{}' ({})", option, item.name, item.id)
            ok
        } catch (_: Throwable) { false }

    /** Finds the first item with any of [names] (contentEquals) and interacts with [option]. */
    fun interact(option: String, vararg names: String): Boolean =
        getItem(*names)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by names failed: option='{}', names={}", option, names.asList())
            false
        }

    /** Finds the first item whose name matches [spred] against any of [names], then interacts. */
    fun interact(spred: (String, CharSequence) -> Boolean, option: String, vararg names: String): Boolean =
        getItem(spred, *names)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by pred failed: option='{}', names={}", option, names.asList())
            false
        }

    /** Finds the first item with any of [ids] and interacts with [option]. */
    fun interact(option: String, vararg ids: Int): Boolean =
        getItem(*ids)?.let { interact(it, option) } ?: run {
            logger.info("[Backpack] Interact by ids failed: option='{}', ids={}", option, ids.asList())
            false
        }
}
