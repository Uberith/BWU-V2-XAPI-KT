@file:Suppress("UNUSED_PARAMETER")

package net.botwithus.kxapi.game.inventory

import net.botwithus.kxapi.script.SuspendableScript
import net.botwithus.xapi.game.inventory.Backpack
import net.botwithus.xapi.game.inventory.Bank
import net.botwithus.xapi.query.ComponentQuery
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

private val logger = LoggerFactory.getLogger(Bank::class.java)

private const val INTERFACE_INDEX = 517
private const val COMPONENT_INDEX = 202

object BankExtensions  {
    
    suspend fun close(script: SuspendableScript): Boolean {
        val res = Bank.close()
        if (res) script.awaitUntil(5) { !Bank.isOpen() }
        return !Bank.isOpen()
    }


    suspend fun loadLastPreset(script: SuspendableScript): Boolean {
        val res = Bank.loadLastPreset()
        if (res) script.awaitTicks(2)
        return res
    }

    suspend fun loadPreset(script: SuspendableScript, presetNumber: Int): Boolean {
        val res = Bank.loadPreset(script, presetNumber)
        if (res) script.awaitTicks(2)
        return res
    }

    suspend fun depositEquipment(script: SuspendableScript): Boolean {
        logger.info("[Bank] suspend depositEquipment(): begin")
        val res = Bank.depositEquipment()
        logger.info("[Bank] suspend depositEquipment(): base call -> {}", res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend depositEquipment(): end -> {}", res)
        return res
    }


    suspend fun depositBackpack(script: SuspendableScript): Boolean {
        logger.info("[Bank] suspend depositBackpack(): begin")
        val res = Bank.depositBackpack()
        logger.info("[Bank] suspend depositBackpack(): base call -> {}", res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend depositBackpack(): end -> {}", res)
        return res
    }


    suspend fun withdrawAll(script: SuspendableScript, name: String): Boolean {
        logger.info("[Bank] suspend withdrawAll(name='{}'): begin", name)
        val res = Bank.withdrawAll(name)
        logger.info("[Bank] suspend withdrawAll(name='{}'): base call -> {}", name, res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, res)
        return res
    }


    suspend fun withdrawAll(script: SuspendableScript, id: Int): Boolean {
        logger.info("[Bank] suspend withdrawAll(id={}): begin", id)
        val res = Bank.withdrawAll(id)
        logger.info("[Bank] suspend withdrawAll(id={}): base call -> {}", id, res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, res)
        return res
    }

    suspend fun emptyBox(script: SuspendableScript, boxName: String, option: String): Boolean {
        val ok = emptyBox(boxName, option)
        if (ok) script.awaitTicks(2)
        return ok
    }

    fun emptyBox(option: String): Boolean {
        if (!Bank.isOpen()) {
            logger.info("[Bank] emptyBox(option='{}'): bank not open", option)
            return false
        }

        val comp = ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
        val ok = comp?.let { it.interact(1) > 0 || it.interact(option) > 0 } ?: false

        logger.info(
            "[Bank] emptyBox(option='{}'): component {} -> {}",
            option,
            if (comp != null) "found" else "not-found",
            ok
        )

        return ok
    }

    fun emptyBox(boxName: String, option: String): Boolean {
        if (!Bank.isOpen()) {
            logger.info("[Bank] emptyBox(name='{}', option='{}'): bank not open", boxName, option)
            return false
        }

        return runCatching {
            // 1) Try interacting with the specific backpack item
            val bpItem = Backpack.getItem({ _, h -> h.toString().contains(boxName,true) }, boxName)
            logger.info("[Bank] emptyBox(name='{}', option='{}'): backpack item -> {}", boxName, option, bpItem?.let { "${it.name} (${it.id})" } ?: "null")

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

            // 2) Try any component with the exact option
            ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()?.let { comp ->
                if (comp.interact(1) > 0 || comp.interact(option) > 0) {
                    logger.info("[Bank] emptyBox(name='{}'): interacted with component by option -> true", boxName)
                    return true
                }
            }

            // 3) Fuzzy search for any 'empty' option containing 'log', 'nest', or 'box'
            var fuzzyOk = false
            ComponentQuery.newQuery(INTERFACE_INDEX).results().forEach { comp ->
                comp.options?.forEach { op ->
                    op?.lowercase()?.takeIf { it.contains("empty") && (it.contains("log") || it.contains("nest") || it.contains("box")) }
                        ?.takeIf { comp.interact(op) > 0 }
                        ?.also {
                            logger.info("[Bank] emptyBox(name='{}'): fuzzy component option '{}' -> true", boxName, op)
                            fuzzyOk = true
                            return@forEach
                        }
                }
            }
            if (fuzzyOk) return true

            // 4) Last-resort: interact with the backpack item directly
            bpItem?.interact(option)
            logger.info("[Bank] emptyBox(name='{}'): fallback backpack interact -> {}", boxName, true)
            true
        }.onFailure { t ->
            logger.warn("[Bank] emptyBox(name='{}') exception: {}", boxName, t.message)
        }.getOrElse { false }
    }



    suspend fun depositAll(script: SuspendableScript, vararg ids: Int) = Bank.depositAll(script,*ids)
    suspend fun depositAllExcept(script: SuspendableScript, vararg ids: Int) = Bank.depositAllExcept(script,*ids)
    suspend fun depositAll(script: SuspendableScript, vararg patterns: Pattern) = Bank.depositAll(script,*patterns)
    suspend fun depositAllExcept(script: SuspendableScript, vararg patterns: Pattern) = Bank.depositAllExcept(script,*patterns)
}