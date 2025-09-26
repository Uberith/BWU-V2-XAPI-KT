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
<<<<<<< HEAD
        fun open(permissive: PermissiveScript): Boolean = JBank.open(permissive)
=======
        fun open(permissive: PermissiveScript) = JBank.open(permissive)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Checks whether the bank widget is currently visible to the local player.
         * @return true when the bank interface is open.
         */
<<<<<<< HEAD
        fun isOpen(): Boolean = JBank.isOpen()
=======
        fun isOpen() = JBank.isOpen()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Attempts to close the bank interface.
         * @return true if the close interaction was executed.
         */
<<<<<<< HEAD
        fun close(): Boolean = JBank.close()
=======
        fun close() = JBank.close()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Loads the most recently used bank preset through the Java API.
         * @return true if the preset load interaction was triggered.
         */
<<<<<<< HEAD
        fun loadLastPreset(): Boolean = JBank.loadLastPreset()
=======
        fun loadLastPreset() = JBank.loadLastPreset()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

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
<<<<<<< HEAD
        fun count(results: ResultSet<InventoryItem>): Int = JBank.count(results)
=======
        fun count(results: ResultSet<InventoryItem>) = JBank.count(results)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
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
<<<<<<< HEAD
        fun isEmpty(): Boolean = JBank.isEmpty()
=======
        fun isEmpty() = JBank.isEmpty()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Executes an interaction on a specific bank slot using an option index.
         * @param slot Zero-based slot index inside the bank container.
         * @param option Interaction option index to trigger.
         * @return true if the interaction was dispatched.
         */
<<<<<<< HEAD
        fun interact(slot: Int, option: Int): Boolean = JBank.interact(slot, option)
=======
        fun interact(slot: Int, option: Int) = JBank.interact(slot, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Tests whether the bank contains items satisfying the given query.
         * @param query Declarative query describing the desired items.
         * @return true if any bank item matches.
         */
<<<<<<< HEAD
        fun contains(query: InventoryItemQuery): Boolean = JBank.contains(query)
=======
        fun contains(query: InventoryItemQuery) = JBank.contains(query)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Checks for the presence of one or more items by name.
         * @param itemNames Display names of the items to look for.
         * @return true if at least one item name is present.
         */
<<<<<<< HEAD
        fun contains(vararg itemNames: String): Boolean = JBank.contains(*itemNames)
=======
        fun contains(vararg itemNames: String) = JBank.contains(*itemNames)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Checks whether any bank item name matches the supplied pattern.
         * @param itemNamePattern Case-insensitive pattern evaluated against item names.
         * @return true if the pattern matches at least one item.
         */
<<<<<<< HEAD
        fun contains(itemNamePattern: Pattern): Boolean = JBank.contains(itemNamePattern)
=======
        fun contains(itemNamePattern: Pattern) = JBank.contains(itemNamePattern)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Counts the total number of items that match any of the provided names.
         * @param itemNames Names to tally.
         * @return Combined quantity of matching items.
         */
<<<<<<< HEAD
        fun getCount(vararg itemNames: String): Int = JBank.getCount(*itemNames)
=======
        fun getCount(vararg itemNames: String) = JBank.getCount(*itemNames)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Counts the number of items with names that satisfy the provided pattern.
         * @param namePattern Regex-style pattern for matching item names.
         * @return Total quantity of items matched by the pattern.
         */
<<<<<<< HEAD
        fun getCount(namePattern: Pattern): Int = JBank.getCount(namePattern)
=======
        fun getCount(namePattern: Pattern) = JBank.getCount(namePattern)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        // ---- FULL withdraw surface (matches Java) ----
        /**
         * Withdraws from the bank using an [InventoryItemQuery] and option index.
         * @param query Query describing the target item.
         * @param option Withdraw option index (for example a quantity shortcut).
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdraw(query: InventoryItemQuery, option: Int): Boolean =
            JBank.withdraw(query, option)
=======
        fun withdraw(query: InventoryItemQuery, option: Int) = JBank.withdraw(query, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Withdraws an item identified by its display name.
         * @param itemName Exact item name to withdraw.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdraw(itemName: String, option: Int): Boolean =
            JBank.withdraw(itemName, option)
=======
        fun withdraw(itemName: String, option: Int) = JBank.withdraw(itemName, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Withdraws an item using its numeric identifier.
         * @param itemId Item ID to withdraw.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdraw(itemId: Int, option: Int): Boolean =
            JBank.withdraw(itemId, option)
=======
        fun withdraw(itemId: Int, option: Int) = JBank.withdraw(itemId, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Withdraws the first item whose name matches the supplied pattern.
         * @param namePattern Pattern evaluated against item names.
         * @param option Withdraw option index or quantity shortcut.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdraw(namePattern: Pattern, option: Int): Boolean =
            JBank.withdraw(namePattern, option)
=======
        fun withdraw(namePattern: Pattern, option: Int) = JBank.withdraw(namePattern, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Withdraws the entire stack of an item by name.
         * @param name Display name of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdrawAll(name: String): Boolean = JBank.withdrawAll(name)
=======
        fun withdrawAll(name: String) = JBank.withdrawAll(name)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Withdraws the entire stack of an item by identifier.
         * @param id Item ID whose contents should be withdrawn.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdrawAll(id: Int): Boolean = JBank.withdrawAll(id)
=======
        fun withdrawAll(id: Int) = JBank.withdrawAll(id)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Withdraws the entire stack of the first item matching the provided pattern.
         * @param pattern Pattern applied to item names.
         * @return true if the withdraw interaction was performed.
         */
<<<<<<< HEAD
        fun withdrawAll(pattern: Pattern): Boolean = JBank.withdrawAll(pattern)
=======
        fun withdrawAll(pattern: Pattern) = JBank.withdrawAll(pattern)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        // ---- Deposits (no-script + script variants) ----
        /**
         * Deposits all items from both backpack and equipment into the bank.
         * @return true if a deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAll(): Boolean = JBank.depositAll()
=======
        fun depositAll() = JBank.depositAll()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Deposits all currently equipped items into the bank.
         * @return true if a deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositEquipment(): Boolean = JBank.depositEquipment()
=======
        fun depositEquipment() = JBank.depositEquipment()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Deposits the entire backpack into the bank.
         * @return true if a deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositBackpack(): Boolean = JBank.depositBackpack()
=======
        fun depositBackpack() = JBank.depositBackpack()
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Deposits items by interacting with the provided component query.
         * @param permissive Script allowed to interact with UI components.
         * @param query Component query targeting the deposit widget.
         * @param option Option index to use on the resolved component.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun deposit(permissive: PermissiveScript, query: ComponentQuery, option: Int): Boolean =
=======
        fun deposit(permissive: PermissiveScript, query: ComponentQuery, option: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.deposit(permissive, query, option)

        /**
         * Deposits all matching items using a pre-resolved component query.
         * @param permissive Script allowed to interact with UI components.
         * @param query Component query resolving the target widget.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAll(permissive: PermissiveScript, query: ComponentQuery): Boolean =
=======
        fun depositAll(permissive: PermissiveScript, query: ComponentQuery) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAll(permissive, query)

        /**
         * Deposits items by directly interacting with a concrete component.
         * @param permissive Script allowed to interact with UI components.
         * @param comp Component to invoke.
         * @param option Option index applied to the component.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun deposit(permissive: PermissiveScript, comp: Component?, option: Int): Boolean =
=======
        fun deposit(permissive: PermissiveScript, comp: Component?, option: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.deposit(permissive, comp, option)

        /**
         * Deposits every item whose name matches any of the supplied values.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemNames Names of items to deposit.
         * @return true if at least one deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAll(permissive: PermissiveScript, vararg itemNames: String): Boolean =
=======
        fun depositAll(permissive: PermissiveScript, vararg itemNames: String) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAll(permissive, *itemNames)

        /**
         * Deposits every item whose identifier is in the supplied list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemIds Identifiers of the items to deposit.
         * @return true if at least one deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAll(permissive: PermissiveScript, vararg itemIds: Int): Boolean =
=======
        fun depositAll(permissive: PermissiveScript, vararg itemIds: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAll(permissive, *itemIds)

        /**
         * Deposits items whose names satisfy any of the provided patterns.
         * @param permissive Script allowed to interact with bank widgets.
         * @param patterns Name patterns to match against backpack items.
         * @return true if at least one deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAll(permissive: PermissiveScript, vararg patterns: Pattern): Boolean =
=======
        fun depositAll(permissive: PermissiveScript, vararg patterns: Pattern) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAll(permissive, *patterns)

        /**
         * Deposits all items except those with names in the provided list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemNames Names of items to keep in the backpack.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAllExcept(permissive: PermissiveScript, vararg itemNames: String): Boolean =
=======
        fun depositAllExcept(permissive: PermissiveScript, vararg itemNames: String) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAllExcept(permissive, *itemNames)

        /**
         * Deposits all items except those whose IDs appear in the exclusion list.
         * @param permissive Script allowed to interact with bank widgets.
         * @param ids Identifiers of items to keep in the backpack.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAllExcept(permissive: PermissiveScript, vararg ids: Int): Boolean =
=======
        fun depositAllExcept(permissive: PermissiveScript, vararg ids: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAllExcept(permissive, *ids)

        /**
         * Deposits all items except those whose names match any exclusion pattern.
         * @param permissive Script allowed to interact with bank widgets.
         * @param patterns Name patterns describing items to keep.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun depositAllExcept(permissive: PermissiveScript, vararg patterns: Pattern): Boolean =
=======
        fun depositAllExcept(permissive: PermissiveScript, vararg patterns: Pattern) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.depositAllExcept(permissive, *patterns)

        /**
         * Deposits a specific item by identifier using the provided option index.
         * @param permissive Script allowed to interact with bank widgets.
         * @param itemId Identifier of the item to deposit.
         * @param option Option index to trigger on the component.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun deposit(permissive: PermissiveScript, itemId: Int, option: Int): Boolean =
=======
        fun deposit(permissive: PermissiveScript, itemId: Int, option: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.deposit(permissive, itemId, option)

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
<<<<<<< HEAD
        ): Boolean = JBank.deposit(permissive, name, spred, option)
=======
        ) = JBank.deposit(permissive, name, spred, option)
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f

        /**
         * Deposits a named item using the supplied option index.
         * @param permissive Script allowed to interact with bank widgets.
         * @param name Display name of the item to deposit.
         * @param option Option index to trigger on the component.
         * @return true if the deposit interaction was performed.
         */
<<<<<<< HEAD
        fun deposit(permissive: PermissiveScript, name: String, option: Int): Boolean =
=======
        fun deposit(permissive: PermissiveScript, name: String, option: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.deposit(permissive, name, option)

        // ---- Presets / vars ----
        /**
         * Loads a preset by number via the underlying Java API.
         * @param permissive Script allowed to interact with bank widgets.
         * @param presetNumber Preset slot identifier (1-based in the UI).
         * @return true if the preset load interaction was performed.
         */
<<<<<<< HEAD
        fun loadPreset(permissive: PermissiveScript, presetNumber: Int): Boolean =
=======
        fun loadPreset(permissive: PermissiveScript, presetNumber: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.loadPreset(permissive, presetNumber)

        /**
         * Reads a varbit value associated with a specific bank slot.
         * @param slot Zero-based bank slot index.
         * @param varbitId Varbit identifier to read.
         * @return The current varbit value, or 0 when unavailable.
         */
<<<<<<< HEAD
        fun getVarbitValue(slot: Int, varbitId: Int): Int =
=======
        fun getVarbitValue(slot: Int, varbitId: Int) =
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            JBank.getVarbitValue(slot, varbitId)

        /**
         * Retrieves the index of the last preset loaded this session.
         * @return Preset index recorded by the Java API, or -1 if none was recorded.
         */
<<<<<<< HEAD
        fun getPreviousLoadedPreset(): Int = JBank.getPreviousLoadedPreset()

        // ---------- Suspend wrappers for convenience ----------
=======
        fun getPreviousLoadedPreset() =
            JBank.getPreviousLoadedPreset()

>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Suspends until the bank has been closed or the timeout elapses.
         *
         * @param script Coroutine-aware script used to await ticks.
         * @return true if the bank is no longer open after awaiting.
         */
        suspend fun close(script: SuspendableScript): Boolean {
<<<<<<< HEAD
            val ok = close()
            if (ok) script.awaitUntil(5) { !isOpen() }
=======
            val closeInitiated = close()
            if (closeInitiated) script.awaitUntil(5) { !isOpen() }
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
            return !isOpen()
        }

        /**
         * Loads the last preset and yields for a couple of ticks to allow the operation to complete.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if the underlying preset load was initiated.
         */
        suspend fun loadLastPreset(script: SuspendableScript): Boolean {
<<<<<<< HEAD
            val ok = loadLastPreset()
            if (ok) script.awaitTicks(2)
            return ok
=======
            val presetLoaded = loadLastPreset()
            if (presetLoaded) script.awaitTicks(2)
            return presetLoaded
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
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
<<<<<<< HEAD
            val ok = loadPreset(permissive, presetNumber)
            if (ok) script.awaitTicks(2)
            return ok
=======
            val presetLoaded = loadPreset(permissive, presetNumber)
            if (presetLoaded) script.awaitTicks(2)
            return presetLoaded
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Deposits equipped items and suspends briefly so the bank view can refresh.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if a deposit interaction was performed.
         */
        suspend fun depositEquipment(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositEquipment(): begin")
<<<<<<< HEAD
            val ok = depositEquipment()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositEquipment(): end -> {}", ok)
            return ok
=======
            val equipmentDeposited = depositEquipment()
            if (equipmentDeposited) script.awaitTicks(1)
            logger.info("[Bank] suspend depositEquipment(): end -> {}", equipmentDeposited)
            return equipmentDeposited
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Deposits the backpack contents and suspends briefly to allow UI updates.
         * @param script Coroutine-aware script used to await ticks.
         * @return true if a deposit interaction was performed.
         */
        suspend fun depositBackpack(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositBackpack(): begin")
<<<<<<< HEAD
            val ok = depositBackpack()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositBackpack(): end -> {}", ok)
            return ok
=======
            val backpackDeposited = depositBackpack()
            if (backpackDeposited) script.awaitTicks(1)
            logger.info("[Bank] suspend depositBackpack(): end -> {}", backpackDeposited)
            return backpackDeposited
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Withdraws all items by name and yields for a tick to synchronise with the client.
         * @param script Coroutine-aware script used to await ticks.
         * @param name Name of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
        suspend fun withdrawAll(script: SuspendableScript, name: String): Boolean {
            logger.info("[Bank] suspend withdrawAll(name='{}'): begin", name)
<<<<<<< HEAD
            val ok = withdrawAll(name)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, ok)
            return ok
=======
            val itemWithdrawn = withdrawAll(name)
            if (itemWithdrawn) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, itemWithdrawn)
            return itemWithdrawn
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Withdraws all items by identifier and yields for a tick to synchronise with the client.
         * @param script Coroutine-aware script used to await ticks.
         * @param id Identifier of the item to withdraw.
         * @return true if the withdraw interaction was performed.
         */
        suspend fun withdrawAll(script: SuspendableScript, id: Int): Boolean {
            logger.info("[Bank] suspend withdrawAll(id={}): begin", id)
<<<<<<< HEAD
            val ok = withdrawAll(id)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, ok)
            return ok
        }

        // ---------- “Empty box” helpers added ----------
=======
            val itemWithdrawn = withdrawAll(id)
            if (itemWithdrawn) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, itemWithdrawn)
            return itemWithdrawn
        }

>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        /**
         * Empties a box-like container (for example bird nests or log boxes) using the provided option and awaits completion.
         * @param script Coroutine-aware script used to await ticks.
         * @param boxName Case-insensitive name fragment identifying the backpack item.
         * @param option Context menu option to invoke when emptying.
         * @return true if the empty interaction was performed.
         */
        suspend fun emptyBox(script: SuspendableScript, boxName: String, option: String): Boolean {
<<<<<<< HEAD
            val ok = emptyBox(boxName, option)
            if (ok) script.awaitTicks(2)
            return ok
=======
            val boxEmptied = emptyBox(boxName, option)
            if (boxEmptied) script.awaitTicks(2)
            return boxEmptied
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Empties the first supported box-type component using an explicit option label.
         * @param option Context menu option text (for example "Empty").
         * @return true if the component interaction succeeded.
         */
        fun emptyBox(option: String): Boolean {
<<<<<<< HEAD
            if (!isOpen()) {
                logger.info("[Bank] emptyBox(option='{}'): bank not open", option)
                return false
            }
            val comp = ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
            val ok = comp?.let { it.interact(1) > 0 || it.interact(option) > 0 } ?: false
            logger.info(
                "[Bank] emptyBox(option='{}'): component {} -> {}",
                option, if (comp != null) "found" else "not-found", ok
            )
            return ok
=======
            if (!isOpen()) return false
            return ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
                ?.let { it.interact(1) > 0 || it.interact(option) > 0 } ?: false
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
        }

        /**
         * Empties a backpack item by name, falling back to fuzzy component searches if necessary.
         * @param boxName Case-insensitive fragment of the item name.
         * @param option Context menu option text (for example "Empty").
         * @return true if the empty interaction succeeded.
         */
        fun emptyBox(boxName: String, option: String): Boolean {
<<<<<<< HEAD
            if (!isOpen()) {
                logger.info("[Bank] emptyBox(name='{}', option='{}'): bank not open", boxName, option)
                return false
            }
            return runCatching {
                val bpItem = Backpack.getItem({ _, h -> h.toString().contains(boxName, true) }, boxName)
                logger.info(
                    "[Bank] emptyBox(name='{}', option='{}'): backpack item -> {}",
                    boxName, option, bpItem?.let { "${it.name} (${it.id})" } ?: "null"
                )

                bpItem?.let {
                    ComponentQuery.newQuery(INTERFACE_INDEX)
                        .id(COMPONENT_INDEX)
                        .itemId(it.id)
                        .results()
                        .firstOrNull()
                        ?.takeIf { comp -> comp.interact(1) > 0 || comp.interact(option) > 0 }
                        ?.also { logger.info("[Bank] emptyBox(name='{}'): interacted with component by id -> true", boxName) }
                        ?.let { return true }
                }

                ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()?.let { comp ->
                    if (comp.interact(1) > 0 || comp.interact(option) > 0) {
                        logger.info("[Bank] emptyBox(name='{}'): interacted with component by option -> true", boxName)
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
                                logger.info("[Bank] emptyBox(name='{}'): fuzzy component option '{}' -> true", boxName, op)
                                fuzzyOk = true
                                return@forEach
                            }
                    }
                }
                if (fuzzyOk) return true

                bpItem?.interact(option)
                logger.info("[Bank] emptyBox(name='{}'): fallback backpack interact -> {}", boxName, true)
                true
            }.onFailure { t ->
                logger.warn("[Bank] emptyBox(name='{}') exception: {}", boxName, t.message)
            }.getOrElse { false }
        }
=======
            if (!isOpen()) return false

            return runCatching {
                val backpackItem = Backpack.getItems().firstOrNull { it.name.contains(boxName, ignoreCase = true) }

                backpackItem?.let { item ->
                    ComponentQuery.newQuery(INTERFACE_INDEX).id(COMPONENT_INDEX).itemId(item.id)
                        .results().firstOrNull()?.let { comp ->
                            if (comp.interact(1) > 0 || comp.interact(option) > 0) return true
                        }
                }

                ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
                    ?.let { if (it.interact(1) > 0 || it.interact(option) > 0) return true }

                if (interactWithEmptyVariant()) return true

                backpackItem?.interact(option) != null
            }.getOrElse { false }
        }

        private fun interactWithEmptyVariant(): Boolean {
            val pattern = Regex("empty.*(log|nest|box)", RegexOption.IGNORE_CASE)
            return ComponentQuery.newQuery(INTERFACE_INDEX).results().any { comp ->
                comp.options.orEmpty().any { option ->
                    option != null && pattern.containsMatchIn(option) && comp.interact(option) > 0
                }
            }
        }
>>>>>>> 8097fc0718d47ffc1a134738963c221e1170542f
    }
}
