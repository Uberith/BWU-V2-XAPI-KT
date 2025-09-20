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
        fun open(permissive: PermissiveScript): Boolean = JBank.open(permissive)

        fun isOpen(): Boolean = JBank.isOpen()
        fun close(): Boolean = JBank.close()
        fun loadLastPreset(): Boolean = JBank.loadLastPreset()

        fun getInventory(): Inventory = JBank.getInventory()
        fun getItems(): Array<Item> = JBank.getItems()
        fun count(results: ResultSet<InventoryItem>): Int = JBank.count(results)
        fun first(query: InventoryItemQuery): Item? = JBank.first(query)
        fun isEmpty(): Boolean = JBank.isEmpty()
        fun interact(slot: Int, option: Int): Boolean = JBank.interact(slot, option)

        fun contains(query: InventoryItemQuery): Boolean = JBank.contains(query)
        fun contains(vararg itemNames: String): Boolean = JBank.contains(*itemNames)
        fun contains(itemNamePattern: Pattern): Boolean = JBank.contains(itemNamePattern)

        fun getCount(vararg itemNames: String): Int = JBank.getCount(*itemNames)
        fun getCount(namePattern: Pattern): Int = JBank.getCount(namePattern)

        // ---- FULL withdraw surface (matches Java) ----
        fun withdraw(query: InventoryItemQuery, option: Int): Boolean =
            JBank.withdraw(query, option)

        fun withdraw(itemName: String, option: Int): Boolean =
            JBank.withdraw(itemName, option)

        fun withdraw(itemId: Int, option: Int): Boolean =
            JBank.withdraw(itemId, option)

        fun withdraw(namePattern: Pattern, option: Int): Boolean =
            JBank.withdraw(namePattern, option)

        fun withdrawAll(name: String): Boolean = JBank.withdrawAll(name)
        fun withdrawAll(id: Int): Boolean = JBank.withdrawAll(id)
        fun withdrawAll(pattern: Pattern): Boolean = JBank.withdrawAll(pattern)

        // ---- Deposits (no-script + script variants) ----
        fun depositAll(): Boolean = JBank.depositAll()
        fun depositEquipment(): Boolean = JBank.depositEquipment()
        fun depositBackpack(): Boolean = JBank.depositBackpack()

        fun deposit(permissive: PermissiveScript, query: ComponentQuery, option: Int): Boolean =
            JBank.deposit(permissive, query, option)

        fun depositAll(permissive: PermissiveScript, query: ComponentQuery): Boolean =
            JBank.depositAll(permissive, query)

        fun deposit(permissive: PermissiveScript, comp: Component?, option: Int): Boolean =
            JBank.deposit(permissive, comp, option)

        fun depositAll(permissive: PermissiveScript, vararg itemNames: String): Boolean =
            JBank.depositAll(permissive, *itemNames)

        fun depositAll(permissive: PermissiveScript, vararg itemIds: Int): Boolean =
            JBank.depositAll(permissive, *itemIds)

        fun depositAll(permissive: PermissiveScript, vararg patterns: Pattern): Boolean =
            JBank.depositAll(permissive, *patterns)

        fun depositAllExcept(permissive: PermissiveScript, vararg itemNames: String): Boolean =
            JBank.depositAllExcept(permissive, *itemNames)

        fun depositAllExcept(permissive: PermissiveScript, vararg ids: Int): Boolean =
            JBank.depositAllExcept(permissive, *ids)

        fun depositAllExcept(permissive: PermissiveScript, vararg patterns: Pattern): Boolean =
            JBank.depositAllExcept(permissive, *patterns)

        fun deposit(permissive: PermissiveScript, itemId: Int, option: Int): Boolean =
            JBank.deposit(permissive, itemId, option)

        fun deposit(
            permissive: PermissiveScript,
            name: String,
            spred: BiFunction<String, CharSequence, Boolean>,
            option: Int
        ): Boolean = JBank.deposit(permissive, name, spred, option)

        fun deposit(permissive: PermissiveScript, name: String, option: Int): Boolean =
            JBank.deposit(permissive, name, option)

        // ---- Presets / vars ----
        fun loadPreset(permissive: PermissiveScript, presetNumber: Int): Boolean =
            JBank.loadPreset(permissive, presetNumber)

        fun getVarbitValue(slot: Int, varbitId: Int): Int =
            JBank.getVarbitValue(slot, varbitId)

        fun getPreviousLoadedPreset(): Int = JBank.getPreviousLoadedPreset()

        // ---------- Suspend wrappers for convenience ----------
        suspend fun close(script: SuspendableScript): Boolean {
            val ok = close()
            if (ok) script.awaitUntil(5) { !isOpen() }
            return !isOpen()
        }

        suspend fun loadLastPreset(script: SuspendableScript): Boolean {
            val ok = loadLastPreset()
            if (ok) script.awaitTicks(2)
            return ok
        }

        suspend fun loadPreset(
            script: SuspendableScript,
            presetNumber: Int,
            permissive: PermissiveScript
        ): Boolean {
            val ok = loadPreset(permissive, presetNumber)
            if (ok) script.awaitTicks(2)
            return ok
        }

        suspend fun depositEquipment(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositEquipment(): begin")
            val ok = depositEquipment()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositEquipment(): end -> {}", ok)
            return ok
        }

        suspend fun depositBackpack(script: SuspendableScript): Boolean {
            logger.info("[Bank] suspend depositBackpack(): begin")
            val ok = depositBackpack()
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend depositBackpack(): end -> {}", ok)
            return ok
        }

        suspend fun withdrawAll(script: SuspendableScript, name: String): Boolean {
            logger.info("[Bank] suspend withdrawAll(name='{}'): begin", name)
            val ok = withdrawAll(name)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, ok)
            return ok
        }

        suspend fun withdrawAll(script: SuspendableScript, id: Int): Boolean {
            logger.info("[Bank] suspend withdrawAll(id={}): begin", id)
            val ok = withdrawAll(id)
            if (ok) script.awaitTicks(1)
            logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, ok)
            return ok
        }

        // ---------- “Empty box” helpers added ----------
        suspend fun emptyBox(script: SuspendableScript, boxName: String, option: String): Boolean {
            val ok = emptyBox(boxName, option)
            if (ok) script.awaitTicks(2)
            return ok
        }

        fun emptyBox(option: String): Boolean {
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
        }

        fun emptyBox(boxName: String, option: String): Boolean {
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
    }
}
