package net.botwithus.kxapi.game.query

import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.util.StringMatchers
import net.botwithus.rs3.inventories.InventoryManager
import net.botwithus.rs3.item.InventoryItem
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Query builder that inspects one or more inventories via [InventoryManager].
 *
 * Pass the relevant inventory ids (backpack, equipment, action bar, etc.) and
 * chain any of the filters before materialising with [results] or iteration.
 *
 * ```kotlin
 * val food = InventoryItemQuery.newQuery(InventoryId.BACKPACK)
 *     .withName("Shark")
 *     .firstMatching()
 * ```
 */
class InventoryItemQuery(vararg inventoryIds: Int) : Query<InventoryItem> {

    private val ids: IntArray = inventoryIds
    internal var root: Predicate<InventoryItem> = Predicate { true }

    companion object {
        /** Creates a query covering the provided inventory ids. */
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

    /** Restricts the query to the supplied slot indices. */
    fun slot(vararg slots: Int): InventoryItemQuery {
        if (slots.isEmpty()) return this
        val prev = root
        val set = slots.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.slot) }
        return this
    }

    /** Matches any items whose id is included in [ids], regardless of slot. */
    fun id(vararg ids: Int): InventoryItemQuery {
        if (ids.isEmpty()) return this
        val prev = root
        val set = ids.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.id) }
        return this
    }

    /**
     * Uses a custom predicate to evaluate the item name. Helpful for case-insensitive
     * or fuzzy comparisons when the exact string is not known ahead of time.
     */
    fun name(name: String, spred: BiFunction<String, CharSequence, Boolean>): InventoryItemQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && spred.apply(name, t.name ?: "") }
        return this
    }

    /** Convenience overload keeping items whose name exactly matches [name]. */
    fun name(name: String): InventoryItemQuery = name(name, BiFunction { a, b -> a.contentEquals(b) })

    /** Accepts several names and keeps items that match any of them. */
    fun name(vararg names: String): InventoryItemQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t -> prev.test(t) && names.any { n -> n.contentEquals(t.name ?: "") } }
        return this
    }

    /** Case-insensitive name comparison against any of the provided values. */
    fun nameEqualsIgnoreCase(vararg names: String): InventoryItemQuery = name(StringMatchers.equalsIgnoreCase, *names)

    /** Partial match helper that keeps items whose name contains the supplied fragments. */
    fun nameContains(vararg fragments: String): InventoryItemQuery = name(StringMatchers.contains, *fragments)

    /** Case-insensitive partial match helper for item names. */
    fun nameContainsIgnoreCase(vararg fragments: String): InventoryItemQuery = name(StringMatchers.containsIgnoreCase, *fragments)

    /** Filters items whose name satisfies the single supplied regular expression. */
    fun name(pattern: Pattern): InventoryItemQuery {
        val prev = root
        root = Predicate { t ->
            val nm = t.name ?: ""
            prev.test(t) && pattern.matcher(nm).find()
        }
        return this
    }

    /**
     * Matches items against any of the provided regex patterns. Use `.*` to model
     * substring searches, mirroring how the XAPI behaves.
     */
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

/** Fluent alias so chains read as `query withName "Shark"`. */
infix fun InventoryItemQuery.withName(name: String): InventoryItemQuery = this.name(name)

