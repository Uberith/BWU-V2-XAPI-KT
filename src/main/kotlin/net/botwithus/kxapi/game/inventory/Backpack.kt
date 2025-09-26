@file:Suppress("unused")

package net.botwithus.kxapi.game.inventory

import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.item.InventoryItem
import net.botwithus.xapi.game.inventory.Backpack as JBackpack
import org.slf4j.LoggerFactory
import java.util.function.BiFunction
import java.util.regex.Pattern

private val logger = LoggerFactory.getLogger(JBackpack::class.java)

/**
 * Import THIS class in callers: net.botwithus.kxapi.game.inventory.Backpack
 * It extends the Java Backpack and re-exposes static-like API via the companion.
 */
class Backpack : JBackpack() {

    companion object {
        // ---------- Pure forwards for existing Java methods ----------
        /**
         * Provides direct access to the live backpack inventory (container id 93).
         * @return Snapshot of the player's backpack as exposed by the Java API.
         */
        fun getInventory(): Inventory = JBackpack.getInventory()

        /**
         * Checks whether every slot in the backpack is currently occupied.
         * @return true when the underlying inventory reports a slot count equal to its capacity.
         */
        fun isFull(): Boolean = JBackpack.isFull()

        /**
         * Determines if the backpack contains no visible items.
         * @return true when all slots are empty according to the Java implementation.
         */
        fun isEmpty(): Boolean = JBackpack.isEmpty()

        /**
         * Retrieves all non-placeholder items present in the backpack.
         * @return List of inventory items returned by the Java layer.
         */
        fun getItems(): List<InventoryItem> = JBackpack.getItems()

        /**
         * Verifies that at least one backpack item name matches the supplied Java predicate.
         * @param spred Java functional interface used to compare item names.
         * @param names Candidate item names evaluated by the predicate.
         * @return true when a name satisfies the predicate.
         */
        fun contains(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): Boolean =
            JBackpack.contains(spred, *names)

        /**
         * Kotlin-friendly overload that wraps [contains] while accepting a Kotlin predicate.
         * @param spred Kotlin lambda invoked for each item/candidate pair.
         * @param names Candidate item names evaluated by the predicate.
         * @return true when a name satisfies the predicate.
         */
        fun contains(spred: (String, CharSequence) -> Boolean, vararg names: String): Boolean =
            contains(BiFunction { name, candidate -> spred(name, candidate) }, *names)

        /**
         * Verifies that the backpack contains an item whose name exactly matches any of the provided values.
         * @param names Display names to locate.
         * @return true when at least one provided name is present.
         */
        fun contains(vararg names: String): Boolean = JBackpack.contains(*names)

        /**
         * Checks for the presence of items by their numerical identifiers.
         * @param ids Item ids to test.
         * @return true if any backpack slot holds one of the identifiers.
         */
        fun contains(vararg ids: Int): Boolean = JBackpack.contains(*ids)

        /**
         * Matches backpack item names using regular expression patterns.
         * @param namePatterns Compiled patterns evaluated against each item name.
         * @return true when a pattern matches at least one backpack item.
         */
        fun contains(vararg namePatterns: Pattern): Boolean = JBackpack.contains(*namePatterns)

        /**
         * Retrieves the first item whose name satisfies the supplied Java predicate against any candidate.
         * @param spred Java functional interface applied to item names.
         * @param names Candidate names used during the comparison.
         * @return Matching inventory item or null when nothing qualifies.
         */
        fun getItem(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): InventoryItem? =
            JBackpack.getItem(spred, *names)

        /**
         * Kotlin-friendly overload that resolves an item using a Kotlin predicate for name comparisons.
         * @param spred Kotlin lambda invoked for each item/candidate pair.
         * @param names Candidate names used during the comparison.
         * @return Matching inventory item or null when nothing qualifies.
         */
        fun getItem(spred: (String, CharSequence) -> Boolean, vararg names: String): InventoryItem? =
            getItem(BiFunction { name, candidate -> spred(name, candidate) }, *names)

        /**
         * Looks up the first item whose display name exactly matches one of the provided values.
         * @param names Display names to scan for.
         * @return Matching inventory item or null when nothing matches.
         */
        fun getItem(vararg names: String): InventoryItem? = JBackpack.getItem(*names)

        /**
         * Resolves the first backpack item whose identifier matches any of the provided values.
         * @param ids Item identifiers to search against.
         * @return Matching inventory item or null when absent.
         */
        fun getItem(vararg ids: Int): InventoryItem? = JBackpack.getItem(*ids)

        // ---------- Kotlin-specific convenience helpers ----------
        /**
         * Issues an interaction on the provided [item] using the desired [option], logging success or failure.
         * @param item Inventory entry to interact with.
         * @param option Context menu option text to invoke.
         * @return true when the interaction call reports success.
         */
        fun interact(item: InventoryItem, option: String): Boolean =
            runCatching {
                logger.info("[Backpack] Interact: option='{}', item='{}' ({})", option, item.name, item.id)
                val success = item.interact(option) > 0
                if (!success) {
                    logger.warn(
                        "[Backpack] Interaction failed: option='{}', item='{}' ({})", option, item.name, item.id
                    )
                }
                success
            }.getOrElse {
                logger.error("[Backpack] Exception during interact", it)
                false
            }

        /**
         * Attempts to interact with the first item whose name equals any of [names].
         * @param option Context menu option text to invoke.
         * @param names Candidate item names searched sequentially.
         * @return true when an interaction was attempted successfully.
         */
        fun interact(option: String, vararg names: String): Boolean =
            getItem(*names)?.let { interact(it, option) } ?: run {
                logger.info("[Backpack] Interact by names failed: option='{}', names={}", option, names.toList())
                false
            }

        /**
         * Attempts to interact with the first item whose name satisfies [spred] against any of [names].
         * @param spred Kotlin predicate used to compare item names.
         * @param option Context menu option text to invoke.
         * @param names Candidate item names evaluated by the predicate.
         * @return true when an interaction was attempted successfully.
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
         * Attempts to interact using [option] with the first item matching one of [ids].
         * @param option Context menu option text to invoke.
         * @param ids Item identifiers searched sequentially.
         * @return true when an interaction was attempted successfully.
         */
        fun interact(option: String, vararg ids: Int): Boolean =
            getItem(*ids)?.let { interact(it, option) } ?: run {
                logger.info("[Backpack] Interact by ids failed: option='{}', ids={}", option, ids.toList())
                false
            }
    }
}
