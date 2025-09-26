package net.botwithus.kxapi.game.traversal

import net.botwithus.kxapi.script.SuspendableScript
import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.rs3.interfaces.Interfaces
import net.botwithus.rs3.vars.VarDomain
import net.botwithus.xapi.game.traversal.enums.LodestoneType
import net.botwithus.xapi.game.traversal.enums.LodestoneType.getInterfaceId
import net.botwithus.xapi.query.ComponentQuery
import org.slf4j.LoggerFactory

object LodestoneNetwork {
    private val logger = LoggerFactory.getLogger("LodestoneNetworkSuspend")

    private const val NETWORK_INTERFACE_ID = 1092
    private const val COMPONENT_QUERY_INTERFACE_ID = 1465
    private const val INTERFACE_OPEN_TIMEOUT_TICKS = 3
    private const val RETRY_DELAY_TICKS = 1
    private const val QUICK_TELEPORT_DELAY_TICKS = 1
    private const val STANDARD_TELEPORT_DELAY_TICKS = 2

    fun isOpen(): Boolean {
        val open = Interfaces.isOpen(NETWORK_INTERFACE_ID)
        if (logger.isDebugEnabled) {
            logger.debug("Lodestone network interface {}", if (open) "is open" else "is not open")
        }
        return open
    }

    fun isAvailable(type: LodestoneType): Boolean {
        val result = VarDomain.getVarBitValue(type.varbitId)
        if (logger.isDebugEnabled) {
            logger.debug(
                "Availability check for {} returned {} (varbit={})",
                type,
                result,
                type.varbitId
            )
        }
        return when (type) {
            LodestoneType.LUNAR_ISLE -> result >= 100
            LodestoneType.BANDIT_CAMP -> result >= 15
            else -> result == 1
        }
    }

    fun open(): Boolean {
        logger.info("Attempting to open the Lodestone network interface")
        val result = ComponentQuery
            .newQuery(COMPONENT_QUERY_INTERFACE_ID)
            .option("Lodestone network")
            .results()
            .firstOrNull()

        if (result == null) {
            logger.warn("Unable to locate the Lodestone network component")
            return false
        }

        val interactionResult = result.interact("Lodestone network")
        if (interactionResult > 0) {
            logger.info("Successfully opened the Lodestone network interface")
            return true
        }

        logger.warn(
            "Failed to open the Lodestone network interface (interaction result: {})",
            interactionResult
        )
        return false
    }

    suspend fun teleport(script: SuspendableScript, type: LodestoneType): Boolean {
        logger.info("Attempting to teleport using {}", type)
        val player = LocalPlayer.self()
        if (player == null) {
            logger.warn("Cannot teleport via {} because the local player is null", type)
            return false
        }

        script.awaitTicks(RETRY_DELAY_TICKS)

        if (!isOpen()) {
            logger.debug("Lodestone network interface is closed. Opening before teleporting via {}", type)
            if (!open()) {
                logger.warn("Failed to open the Lodestone network interface for {}", type)
                return false
            }
            val waitTicks = INTERFACE_OPEN_TIMEOUT_TICKS
            if (!script.awaitUntil(waitTicks) { isOpen() }) {
                logger.debug(
                    "Waiting up to {} ticks for the Lodestone network interface to open for {} timed out",
                    waitTicks,
                    type
                )
                return false
            }
        }

        logger.debug("Lodestone network interface open; locating component for {}", type)
        val interfaceId = getInterfaceId()
        val component = Interfaces.getComponent(interfaceId, type.componentId)
        if (component == null) {
            logger.debug(
                "Teleport component for {} not yet available (interfaceId={}, componentId={}); delaying before retrying",
                type,
                interfaceId,
                type.componentId
            )
            script.awaitTicks(RETRY_DELAY_TICKS)
            return false
        }

        var interactionResult = component.interact("Teleport")
        val teleportAction = type.teleportAction
        if (interactionResult <= 0 && !teleportAction.isNullOrBlank()) {
            interactionResult = component.interact(teleportAction)
        }
        if (interactionResult <= 0) {
            interactionResult = component.interact()
        }
        if (interactionResult <= 0) {
            logger.warn(
                "Failed to interact with {} (interaction result: {}). Retrying after short delay.",
                type,
                interactionResult
            )
            script.awaitTicks(RETRY_DELAY_TICKS)
            return false
        }

        val wax = VarDomain.getVarBitValue(28623)
        val quick = VarDomain.getVarBitValue(28622)
        if (logger.isDebugEnabled) {
            logger.debug(
                "Teleport interaction succeeded for {} (quick={}, wax={})",
                type,
                quick,
                wax
            )
        }
        if (quick == 1 && wax > 0) {
            script.awaitTicks(QUICK_TELEPORT_DELAY_TICKS)
        } else {
            script.awaitTicks(STANDARD_TELEPORT_DELAY_TICKS)
        }
        logger.info("Teleport initiated for {}", type)
        return true
    }

    fun teleportToPreviousDestination(): Boolean {
        logger.info("Attempting to teleport to the previous destination")
        val result = ComponentQuery
            .newQuery(COMPONENT_QUERY_INTERFACE_ID)
            .option("Previous Destination")
            .results()
            .firstOrNull()

        if (result == null) {
            logger.warn("Unable to locate the Previous Destination component")
            return false
        }

        val interactionResult = result.interact("Previous Destination")
        if (interactionResult > 0) {
            logger.info("Teleport to the previous destination initiated")
            return true
        }

        logger.warn(
            "Failed to teleport to the previous destination (interaction result: {})",
            interactionResult
        )
        return false
    }
}
