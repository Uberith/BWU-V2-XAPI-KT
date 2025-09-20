@file:Suppress("UNUSED_PARAMETER")

package net.botwithus.kxapi.game.inventory

import net.botwithus.kxapi.script.SuspendableScript
import net.botwithus.xapi.game.inventory.Backpack
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.query.InventoryItemQuery
import net.botwithus.xapi.query.result.ResultSet
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import java.util.function.BiFunction
import net.botwithus.rs3.interfaces.Component
import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.item.InventoryItem
import net.botwithus.rs3.item.Item
import net.botwithus.xapi.game.inventory.Bank as JBank
import net.botwithus.xapi.script.permissive.base.PermissiveScript

private val logger = LoggerFactory.getLogger(JBank::class.java)

private const val INTERFACE_INDEX = 517
private const val COMPONENT_INDEX = 202

/**
 * Import THIS class in callers: net.botwithus.kxapi.game.inventory.Bank
 * It extends the Java Bank and re-exposes static-like API via the companion.
 */
class Bank : JBank() {

    companion object {
        // ---------- Pure forwards for existing Java methods ----------
        /**
         * Attempts to open the bank using the provided [PermissiveScript].
         *
         * The permissive script drives how the interaction is performed (pathing, clicking, etc.).
         * @param permissive Script wrapper that is allowed to interact with blocking UI states.
         * @return true when the bank was already open or an open interaction was successfully issued.
         */
        fun open(permissive: PermissiveScript) = JBank.open(permissive)

        /**
         * Checks whether the bank widget is currently visible to the local player.
         * @return true when the bank interface is open.
         */
        fun isOpen() = JBank.isOpen()
        /**
         * Attempts to close the bank interface.
         * @return true if the close interaction was executed.
         */
        fun close() = JBank.close()
        /**
         * Loads the most recently used bank preset through the Java API.
         * @return true if the preset load interaction was triggered.
         */
        fun loadLastPreset() = JBank.loadLastPreset()

        /**
         * Provides direct access to the active bank inventory interface.
         * @return The live [Inventory] backing the open bank.
         */
        fun getInventory(): Inventory = JBank.getInventory()
        /**
         * Returns the snapshot of items currently visible within the bank.
         * @return Array of items as exposed by the Java API.
         */
        fun getItems(): Array<Item> = JBank.getItems()
        /**
         * Counts the number of items in the bank that match a supplied query result set.
         * @param results Resolved bank items to aggregate.
         * @return Count of matching items.
         */
        fun count(results: ResultSet<InventoryItem>) = JBank.count(results)
        /**
         * Finds the first item in the bank that satisfies the provided query.
         * @param query Builder describing the desired inventory item.
         * @return The first matching [Item], or null if nothing matches.
         */
        fun first(query: InventoryItemQuery): Item? = JBank.first(query)
        /**
         * Checks whether the bank currently has no withdrawable items.
         * @return true if the bank contains zero items.
         */
        fun isEmpty() = JBank.isEmpty()
        /**
         * Executes an interaction on a specific bank slot using an option index.
         * @param slot Zero-based slot index inside the bank container.
         * @param option Interaction option index to trigger.
         * @return true if the interaction was dispatched.
         */
        fun interact(slot: Int, option: Int) = JBank.interact(slot, option)

        /**
         * Tests whether the bank contains items satisfying the given query.
         * @param query Declarative query describing the desired items.
         * @return true if any bank item matches.
         */
        fun contains(query: InventoryItemQuery) = JBank.contains(query)
        /**
         * Checks for the presence of one or more items by name.
         * @param itemNames Display names of the items to look for.
         * @return true if at least one item name is present.
         */
        fun contains(vararg itemNames: String) = JBank.contains(*itemNames)
        /**
         * Checks whether any bank item name matches the supplied pattern.
         * @param itemNamePattern Case-insensitive pattern evaluated against item names.
         * @return true if the pattern matches at least one item.
         */
        fun contains(itemNamePattern: Pattern) = JBank.contains(itemNamePattern)

        /**
         * Counts the total number of items that match any of the provided names.
         * @param itemNames Names to tally.
         * @return Combined quantity of matching items.
         */
        fun getCount(vararg itemNames: String) = JBank.getCount(*itemNames)
        /**
         * Counts the number of items with names that satisfy the provided pattern.
         * @param namePattern Regex-style pattern for matching item names.
         * @return Total quantity of items matched by the pattern.
         */
        fun getCount(namePattern: Pattern) = JBank.getCount(namePattern)

        // ---- FULL withdraw surface (matches Java) ----
        /**
         * Withdraws from the bank using an [InventoryItemQuery] and option index.
         * @param query Query describing the target item.
         * @param option Withdraw option index (for example a quantity shortcut).
         * @return true if the withdraw interaction was performed.
         */
        fun withdraw(query: InventoryItemQuery, option: Int) = JBank.withdraw(query, option)

        /**
         * Withdraws an item identified by its display name.
         * @param itemName Exact item name to withdraw.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
        fun withdraw(itemName: String, option: Int) = JBank.withdraw(itemName, option)

        /**
         * Withdraws an item using its numeric identifier.
         * @param itemId Item ID to withdraw.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
        fun withdraw(itemId: Int, option: Int) = JBank.withdraw(itemId, option)

        /**
         * Withdraws the first item whose name matches the supplied pattern.
         * @param namePattern Pattern evaluated against item names.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
        fun withdraw(namePattern: Pattern, option: Int) = JBank.withdraw(namePattern, option)

        /**
         * Withdraws the entire stack of an item by name.
         * @param name Display name of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
        fun withdrawAll(name: String) = JBank.withdrawAll(name)
        /**
         * Withdraws the entire stack of an item by identifier.
         * @param id Item ID whose contents should be withdrawn.
         * @return true if the withdraw interaction was performed.
         */
        fun withdrawAll(id: Int) = JBank.withdrawAll(id)
        /**
         * Withdraws the entire stack of the first item matching the provided pattern.
         * @param pattern Pattern applied to item names.
         * @return true if the withdraw interaction was performed.
         */
        fun withdrawAll(pattern: Pattern) = JBank.withdrawAll(pattern)

        // ---- Deposits (no-script + script variants) ----
        /**
         * Deposits all items from both backpack and equipment into the bank.
         * @return true if a deposit interaction was performed.
         */
        fun depositAll() = JBank.depositAll()
        /**
         * Deposits all currently equipped items into the bank.
         * @return true if a deposit interaction was performed.
         */
        fun depositEquipment() = JBank.depositEquipment()
        /**
         * Deposits the entire backpack into the bank.
         * @return true if a deposit interaction was performed.
         */
        fun depositBackpack() = JBank.depositBackpack()

        /**
         * Deposits items by interacting with the provided component query.
         * @param permissive Script allowed to interact with UI components.
         * @param query Component query targeting the deposit widget.
         * @param option Option index to use on the resolved component.
         * @return true if the deposit interaction was performed.
         */
        fun deposit(permissive: PermissiveScript, query: ComponentQuery, option: Int) = JBank.deposit(permissive, query, option)

        /**
         * Deposits all matching items using a pre-resolved component query.
         * @param permissive Script allowed to interact with UI components.
         * @param query Component query resolving the target widget.
         * @return true if the deposit interaction was performed.
         */
        fun depositAll(permissive: PermissiveScript, query: ComponentQuery) = JBank.depositAll(permissive, query)

        /**
         * Deposits items by directly interacting with a concrete component.
         * @param permissive Script allowed to interact with UI components.
         * @param comp Component to invoke.
         * @param option Option index applied to the component.
         * @return true if the deposit interaction was performed.
         */
        fun deposit(permissive: PermissiveScript, comp: Component?, option: Int) = JBank.deposit(permissive, comp, option)

        /**
         * Deposits every item whose name matches any of the supplied values.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemNames Names of items to deposit.
         * @return true if at least one deposit interaction was performed.
         */
        fun depositAll(permissive: PermissiveScript, vararg itemNames: String) = JBank.depositAll(permissive, *itemNames)

        /**
         * Deposits every item whose identifier is in the supplied list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemIds Identifiers of the items to deposit.
         * @return true if at least one deposit interaction was performed.
         */
        fun depositAll(permissive: PermissiveScript, vararg itemIds: Int) = JBank.depositAll(permissive, *itemIds)

        /**
         * Deposits items whose names satisfy any of the provided patterns.
         * @param permissive Script allowed to interact with bank widgets.
         * @param patterns Name patterns to match against backpack items.
         * @return true if at least one deposit interaction was performed.
         */
        fun depositAll(permissive: PermissiveScript, vararg patterns: Pattern) = JBank.depositAll(permissive, *patterns)

        /**
         * Deposits all items except those with names in the provided list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemNames Names of items to keep in the backpack.
         * @return true if the deposit interaction was performed.
         */
        fun depositAllExcept(permissive: PermissiveScript, vararg itemNames: String) = JBank.depositAllExcept(permissive, *itemNames)

        /**
         * Deposits all items except those whose IDs appear in the exclusion list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param ids Identifiers of items to keep in the backpack.
         * @return true if the deposit interaction was performed.
         */
        fun depositAllExcept(permissive: PermissiveScript, vararg ids: Int) = JBank.depositAllExcept(permissive, *ids)

        /**
         * Deposits all items except those whose names match any exclusion pattern.
         * @param permissive Script allowed to interact with bank widgets.
         * @param patterns Name patterns describing items to keep.
         * @return true if the deposit interaction was performed.
         */
        fun depositAllExcept(permissive: PermissiveScript, vararg patterns: Pattern) = JBank.depositAllExcept(permissive, *patterns)

        /**
         * Deposits a specific item by identifier using the provided option index.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemId Identifier of the item to deposit.
         * @param option Option index to trigger on the component.
         * @return true if the deposit interaction was performed.
         */
        fun deposit(permissive: PermissiveScript, itemId: Int, option: Int) = JBank.deposit(permissive, itemId, option)

        /**
         * Deposits a named item using a custom string comparison function.
         * @param permissive Script allowed to interact with bank widgets.
         * @param name Display name used for matching.
         * @param spred Function that determines whether a component option matches the provided name.
         * @param option Option index to trigger on the resolved component.
         * @return true if the deposit interaction was performed.
         */
        fun deposit(
            permissive: PermissiveScript,
            name: String,
            spred: BiFunction<String, CharSequence, Boolean>,
            option: Int
        ) = JBank.deposit(permissive, name, spred, option)

        /**
         * Deposits a named item using the supplied option index.
         * @param permissive Script allowed to interact with bank widgets.
         * @param name Display name of the item to deposit.
         * @param option Option index to trigger on the component.
         * @return true if the deposit interaction was performed.
         */
        fun deposit(permissive: PermissiveScript, name: String, option: Int) = JBank.deposit(permissive, name, option)

        // ---- Presets / vars ----
        /**
         * Loads a preset by number via the underlying Java API.
         * @param permissive Script allowed to interact with bank widgets.
         * @param presetNumber Preset slot identifier (1-based in the UI).
         * @return true if the preset load interaction was performed.
         */
        fun loadPreset(permissive: PermissiveScript, presetNumber: Int) = JBank.loadPreset(permissive, presetNumber)

        /**
         * Reads a varbit value associated with a specific bank slot.
         * @param slot Zero-based bank slot index.
         * @param varbitId Varbit identifier to read.
         * @return The current varbit value, or 0 when unavailable.
         */
        fun getVarbitValue(slot: Int, varbitId: Int) = JBank.getVarbitValue(slot, varbitId)

        /**
         * Retrieves the index of the last preset loaded this session.
         * @return Preset index recorded by the Java API, or -1 if none was recorded.
         */
        fun getPreviousLoadedPreset() = JBank.getPreviousLoadedPreset()

        // ---------- Suspend wrappers for convenience ----------
        /**
         * Suspends until the bank has been closed or the timeout elapses.
         *
         * @param script Coroutine-aware script used to await ticks.
         * @return true if the bank is no longer open after awaiting.
         */
        suspend fun close(script: SuspendableScript): Boolean {
            val ok = close()
            if (ok) script.awaitUntil(5) { !isOpen() }
            return !isOpen()
        }

        /**
         * Loads the last preset and yields for a couple of ticks to allow the operation to complete.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if the underlying preset load was initiated.
         */
        suspend fun loadLastPreset(script: SuspendableScript): Boolean {
            val ok = loadLastPreset()
            if (ok) script.awaitTicks(2)
            return ok
        }

        /**
         * Loads a specific preset and yields briefly so the backpack can update.
         * @param script Coroutine-aware script used to await ticks.
         * @param presetNumber Preset slot identifier (1-based in the UI).
         * @param permissive Script allowed to interact with bank widgets.
         * @return true if the underlying preset load was initiated.
         */
        suspend fun loadPreset(
            script: SuspendableScript,
            presetNumber: Int,
            permissive: PermissiveScript
        ): Boolean {
            val ok = loadPreset(permissive, presetNumber)
            if (ok) script.awaitTicks(2)
            return ok
        }

        /**
         * Deposits equipped items and suspends briefly so the bank view can refresh.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if a deposit interaction was performed.
         */
        suspend fun depositEquipment(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositEquipment(): begin")
            val ok = depositEquipment()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositEquipment(): end -> {}", ok)
            return ok
        }

        /**
         * Deposits the backpack contents and suspends briefly to allow UI updates.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if a deposit interaction was performed.
         */
        suspend fun depositBackpack(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositBackpack(): begin")
            val ok = depositBackpack()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositBackpack(): end -> {}", ok)
            return ok
        }

        /**
         * Withdraws all items by name and yields for a tick to synchronise with the client.
         * @param script Coroutine-aware script used to await ticks.
         * @param name Name of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
        suspend fun withdrawAll(script: SuspendableScript, name: String): Boolean {
            logger.info("[Bank] suspend withdrawAll(name='{}'): begin", name)
            val ok = withdrawAll(name)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, ok)
            return ok
        }

        /**
         * Withdraws all items by identifier and yields for a tick to synchronise with the client.
         * @param script Coroutine-aware script used to await ticks.
         * @param id Identifier of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
        suspend fun withdrawAll(script: SuspendableScript, id: Int): Boolean {
            logger.info("[Bank] suspend withdrawAll(id={}): begin", id)
            val ok = withdrawAll(id)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, ok)
            return ok
        }

        // ---------- “Empty box” helpers added ----------
        /**
         * Empties a box-like container (for example bird nests or log boxes) using the provided option and awaits completion.
         * @param script Coroutine-aware script used to await ticks.
         * @param boxName Case-insensitive name fragment identifying the backpack item.
         * @param option Context menu option to invoke when emptying.
         * @return true if the empty interaction was performed.
         */
        suspend fun emptyBox(script: SuspendableScript, boxName: String, option: String): Boolean {
            val ok = emptyBox(boxName, option)
            if (ok) script.awaitTicks(2)
            return ok
        }

        /**
         * Empties the first supported box-type component using an explicit option label.
         * @param option Context menu option text (for example "Empty").
         * @return true if the component interaction succeeded.
         */
        fun emptyBox(option: String): Boolean {
            if (!isOpen()) {
                logger.debug("emptyBox(option='{}'): bank not open", option)
                return false
            }
            val comp = ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
            val ok = comp?.let { it.interact(1) > 0 || it.interact(option) > 0 } ?: false
            logger.debug(
                "emptyBox(option='{}'): component {} -> {}",
                option, if (comp != null) "found" else "not-found", ok
            )
            return ok
        }

        /**
         * Empties a backpack item by name, falling back to fuzzy component searches if necessary.
         * @param boxName Case-insensitive fragment of the item name.
         * @param option Context menu option text (for example "Empty").
         * @return true if the empty interaction succeeded.
         */
        fun emptyBox(boxName: String, option: String): Boolean {
            if (!isOpen()) {
                logger.debug("emptyBox(name='{}', option='{}'): bank not open", boxName, option)
                return false
            }
            return runCatching {
                val bpItem = Backpack.getItem({ _, h -> h.toString().contains(boxName, true) }, boxName)
                logger.debug(
                    "emptyBox(name='{}', option='{}'): backpack item -> {}",
                    boxName, option, bpItem?.let { "${it.name} (${it.id})" } ?: "null"
                )

                bpItem?.let {
                    ComponentQuery.newQuery(INTERFACE_INDEX)
                        .id(COMPONENT_INDEX)
                        .itemId(it.id)
                        .results()
                        .firstOrNull()
                        ?.takeIf { comp -> comp.interact(1) > 0 || comp.interact(option) > 0 }
                        ?.also { logger.debug("emptyBox(name='{}'): interacted with component by id -> true", boxName) }
                        ?.let { return true }
                }

                ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()?.let { comp ->
                    if (comp.interact(1) > 0 || comp.interact(option) > 0) {
                        logger.debug("emptyBox(name='{}'): interacted with component by option -> true", boxName)
                        return true
                    }
                }

                var fuzzyOk = false
                ComponentQuery.newQuery(INTERFACE_INDEX).results().forEach { comp ->
                    comp.options?.forEach { op ->
                        op?.lowercase()
                            ?.takeIf { it.contains("empty") && (it.contains("log") || it.contains("nest") || it.contains("box")) }
                            ?.takeIf { comp.interact(op) > 0 }
                            ?.also {
                                logger.debug("emptyBox(name='{}'): fuzzy component option '{}' -> true", boxName, op)
                                fuzzyOk = true
                                return@forEach
                            }
                    }
                }
                if (fuzzyOk) return true

                bpItem?.interact(option)
                logger.debug("emptyBox(name='{}'): fallback backpack interact -> {}", boxName, true)
                true
            }.onFailure { t ->
                logger.warn("emptyBox(name='{}') exception: {}", boxName, t.message)
            }.getOrElse { false }
        }
    }
}
