@file:Suppress("UNUSED_PARAMETER")

package net.botwithus.kxapi.game.inventory

import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.item.InventoryItem
import net.botwithus.xapi.game.inventory.Backpack as JBackpack
import org.slf4j.LoggerFactory
import java.util.function.BiFunction
import java.util.regex.Pattern

private val logger = LoggerFactory.getLogger(JBackpack::class.java)

/**
 * Import THIS class: net.botwithus.kxapi.game.inventory.Backpack
 * - Extends the Java Backpack
 * - Re-exports the entire static API via companion forwards
 * - Includes convenience interact(...) helpers you wrote
 */
class Backpack : JBackpack() {

    companion object {
        // -------- Pure forwards of the Java static API (no @JvmStatic to avoid clashes) --------
        fun getInventory(): Inventory = JBackpack.getInventory()
        fun isFull(): Boolean = JBackpack.isFull()
        fun isEmpty(): Boolean = JBackpack.isEmpty()
        fun getItems(): List<InventoryItem> = JBackpack.getItems()

        // contains(...)
        fun contains(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): Boolean =
            JBackpack.contains(spred, *names)
        fun contains(vararg names: String): Boolean =
            JBackpack.contains(*names)
        fun contains(vararg ids: Int): Boolean =
            JBackpack.contains(*ids)
        fun contains(vararg namePatterns: Pattern): Boolean =
            JBackpack.contains(*namePatterns)

        // getItem(...)
        fun getItem(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): InventoryItem? =
            JBackpack.getItem(spred, *names)
        fun getItem(vararg names: String): InventoryItem? =
            JBackpack.getItem(*names)
        fun getItem(vararg ids: Int): InventoryItem? =
            JBackpack.getItem(*ids)

        // -------- Kotlin-friendly overloads that delegate to the BiFunction versions --------
        fun contains(spred: (String, CharSequence) -> Boolean, vararg names: String): Boolean =
            contains(BiFunction(spred), *names)

        fun getItem(spred: (String, CharSequence) -> Boolean, vararg names: String): InventoryItem? =
            getItem(BiFunction(spred), *names)

        // -------- Your convenience helpers (ported from BackpackExtensions) --------
        /**
         * Attempts to interact with [item] using the given [option].
         * @return true if the interaction succeeded, false otherwise.
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

        /** Finds the first item whose name equals any of [names] and interacts with it using [option]. */
        fun interact(option: String, vararg names: String): Boolean =
            getItem(*names)?.let { interact(it, option) } ?: run {
                logger.info("[Backpack] Interact by names failed: option='{}', names={}", option, names.toList())
                false
            }

        /** Finds the first item whose name matches [spred] against any of [names], then interacts with [option]. */
        fun interact(
            spred: (String, CharSequence) -> Boolean,
            option: String,
            vararg names: String
        ): Boolean =
            getItem(spred, *names)?.let { interact(it, option) } ?: run {
                logger.info("[Backpack] Interact by predicate failed: option='{}', names={}", option, names.toList())
                false
            }

        /** Finds the first item with any of [ids] and interacts with [option]. */
        fun interact(option: String, vararg ids: Int): Boolean =
            getItem(*ids)?.let { interact(it, option) } ?: run {
                logger.info("[Backpack] Interact by ids failed: option='{}', ids={}", option, ids.toList())
                false
            }

        // --- Helper to adapt Kotlin function to BiFunction without extra alloc sites all over ---
        private fun BiFunction(f: (String, CharSequence) -> Boolean): BiFunction<String, CharSequence, Boolean> =
            BiFunction { a, b -> f(a, b) }
    }
}
