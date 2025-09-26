package net.botwithus.kxapi.game.skilling.production.ui.impl.smithing

import net.botwithus.kxapi.game.skilling.production.ProductionMessage
import net.botwithus.kxapi.game.skilling.production.ProductionResult
import net.botwithus.kxapi.game.skilling.production.toMessage
import net.botwithus.kxapi.game.skilling.production.ui.ProductionInterfaceBase
import net.botwithus.kxapi.util.componentIndex
import net.botwithus.kxapi.util.findItemByName
import net.botwithus.kxapi.util.subComponentIndex
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.script.BwuScript

class MetalInterface private constructor(
    private val script: BwuScript,
    private val outputName: String,
    private val inputName: String,
    private val modifier: SmithingBaseModifier,
    private val smithingMode: Boolean
) : ProductionInterfaceBase() {

    companion object {
        const val MAKE_X_INTERFACE = 37
        const val SELECTED_ITEM_INDEX = 40

        fun forSmelting(script: BwuScript, settings: SmeltingProduction): MetalInterface {
            return MetalInterface(
                script = script,
                outputName = settings.outputName,
                inputName = settings.inputName,
                modifier = SmithingBaseModifier.BASE,
                smithingMode = false
            )
        }

        fun forSmithing(script: BwuScript, settings: SmithingProduction): MetalInterface {
            return MetalInterface(
                script = script,
                outputName = settings.outputName,
                inputName = settings.inputName,
                modifier = settings.modifier,
                smithingMode = true
            )
        }
    }

    fun parseItemName(raw: String): Pair<String, SmithingBaseModifier> {
        val trimmed = raw.trim()

        if (trimmed.contains("burial", ignoreCase = true)) {
            return trimmed to SmithingBaseModifier.BURIAL
        }

        val plusMatch = Regex(""" \+\s*(\d+)$""").find(trimmed)

        return if (plusMatch != null) {
            val baseName = trimmed.removeSuffix(plusMatch.value).trim()
            val modifier = when (plusMatch.groupValues[1].toInt()) {
                1 -> SmithingBaseModifier.PLUS_1
                2 -> SmithingBaseModifier.PLUS_2
                3 -> SmithingBaseModifier.PLUS_3
                4 -> SmithingBaseModifier.PLUS_4
                5 -> SmithingBaseModifier.PLUS_5
                else -> SmithingBaseModifier.BASE
            }
            baseName to modifier
        } else {
            trimmed to SmithingBaseModifier.BASE
        }
    }

    override fun handle(): ProductionMessage<ProductionResult>? {
        val selectedItemText = ComponentQuery.newQuery(MAKE_X_INTERFACE)
            .componentIndex(SELECTED_ITEM_INDEX)
            .first()
            .text

        val (displayName, selectedModifier) = parseItemName(selectedItemText)

        return when (displayName) {
            outputName -> handleSelectedOutput(selectedModifier)
            else -> selectProduct()
        }
    }

    private fun handleSelectedOutput(selectedModifier: SmithingBaseModifier): ProductionMessage<ProductionResult>? {
        return when {
            smithingMode && modifier != selectedModifier -> selectModifierComponent()
            canProduce() -> {
                ComponentQuery.newQuery(MAKE_X_INTERFACE).componentIndex(163).first().interact()
                ProductionResult.CREATION_IN_PROGRESS.toMessage()
            }
            else -> ProductionResult.MISSING_REQUIREMENTS.toMessage()
        }
    }

    private fun selectModifierComponent(): ProductionMessage<ProductionResult> {
        val component = ComponentQuery.newQuery(MAKE_X_INTERFACE)
            .componentIndex(modifier.componentID)
            .first()

        return if (component.isHidden) {
            ProductionResult.Smithing.SELECTING_BASE_MODIFIER_NOT_SUPPORTED.toMessage("Cannot find ${modifier.name}")
        } else {
            component.interact()
            ProductionResult.Smithing.SELECTING_BASE_MODIFIER.toMessage("Selecting Base ${modifier.name}")
        }
    }

    private fun selectProduct(): ProductionMessage<ProductionResult> {
        ComponentQuery.newQuery(MAKE_X_INTERFACE).findItemByName(outputName, ::getItemName)?.interact() ?: run {
            ComponentQuery.newQuery(MAKE_X_INTERFACE).findItemByName(inputName, ::getItemName)?.interact()
                ?: return ProductionResult.Smithing.INPUT_NOT_FOUND.toMessage("Cannot find $inputName")
            }
        return ProductionResult.INTERFACE_ERROR.toMessage()
    }

    override fun makeXInterface(): Int = MAKE_X_INTERFACE

    override fun canProduce(): Boolean {
        return ComponentQuery.newQuery(MAKE_X_INTERFACE)
            .componentIndex(34)
            .subComponentIndex(4)?.text?.toInt() != 0
    }
}
