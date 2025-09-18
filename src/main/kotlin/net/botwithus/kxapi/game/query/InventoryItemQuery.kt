package net.botwithus.kxapi.game.query

import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.internal.ResultSet
import net.botwithus.rs3.inventories.InventoryManager
import net.botwithus.rs3.item.InventoryItem
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Inventory item query over one or more inventory ids.
 */
class InventoryItemQuery(vararg inventoryIds: Int) : Query<InventoryItem> {

    private val ids: IntArray = inventoryIds
    internal var root: Predicate<InventoryItem> = Predicate { true }

    companion object {
        @JvmStatic
        fun newQuery(vararg inventoryIds: Int): InventoryItemQuery = InventoryItemQuery(*inventoryIds)
    }

    override fun results(): ResultSet<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        for (id in ids) {
            try {
                val inv = InventoryManager.getInventory(id) ?: continue
                val list = inv.items?.filter { root.test(it) } ?: emptyList()
                items.addAll(list)
            } catch (_: Throwable) { }
        }
        return ResultSet(items)
    }

    override fun iterator(): MutableIterator<InventoryItem> = results().iterator()

    override fun test(inventoryItem: InventoryItem): Boolean = root.test(inventoryItem)

    /** Filter by slot numbers. */
    fun slot(vararg slots: Int): InventoryItemQuery {
        if (slots.isEmpty()) return this
        val prev = root
        val set = slots.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.slot) }
        return this
    }

    /** Filter by item ids. */
    fun id(vararg ids: Int): InventoryItemQuery {
        if (ids.isEmpty()) return this
        val prev = root
        val set = ids.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.id) }
        return this
    }

    /** Filter by exact item name with custom predicate. */
    fun name(name: String, spred: BiFunction<String, CharSequence, Boolean>): InventoryItemQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && spred.apply(name, t.name ?: "") }
        return this
    }

    fun name(name: String): InventoryItemQuery = name(name, BiFunction { a, b -> a.contentEquals(b) })

    /** Filter by any of the provided item names. */
    fun name(vararg names: String): InventoryItemQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t -> prev.test(t) && names.any { n -> n.contentEquals(t.name ?: "") } }
        return this
    }

    /** Filter by regex pattern on item name. */
    fun name(pattern: Pattern): InventoryItemQuery {
        val prev = root
        root = Predicate { t ->
            val nm = t.name ?: ""
            prev.test(t) && pattern.matcher(nm).find()
        }
        return this
    }

    /** Filter by any of multiple regex patterns on item name. */
    fun name(vararg patterns: Pattern): InventoryItemQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val nm = t.name ?: ""
            prev.test(t) && patterns.any { p -> p.matcher(nm).find() }
        }
        return this
    }
}