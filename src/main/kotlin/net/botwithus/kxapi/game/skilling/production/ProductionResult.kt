package net.botwithus.kxapi.game.skilling.production

/**
 * Wrapper class for production results with dynamic messaging
 */
data class ProductionMessage<T : ProductionResult>(
    val result: T,
    val customMessage: String? = null,
    val details: Map<String, Any> = emptyMap()
) {
    val message: String get() = when {
        customMessage != null -> customMessage
        else -> result.defaultMessage
    }

    fun withMessage(message: String) = copy(customMessage = message)
    fun withDetails(vararg pairs: Pair<String, Any>) = copy(details = details + pairs)
    fun withDetails(newDetails: Map<String, Any>) = copy(details = details + newDetails)
}

/**
 * Base class for production results with default messaging.
 * Provides common result types and allows for custom messages.
 */
open class ProductionResult(val defaultMessage: String) {
    
    companion object {
        val CREATION_IN_PROGRESS: ProductionResult get() = ProductionResult("Creation In Progress")
        val INTERFACE_ERROR: ProductionResult get() = ProductionResult("Interface error")
        val MISSING_REQUIREMENTS: ProductionResult get() = ProductionResult("Missing requirements")
    }

    /**
     * Production result specific to smithing activities.
     */
    class Smithing(defaultMessage: String) : ProductionResult(defaultMessage) {
        companion object {
            val INPUT_NOT_FOUND: Smithing get() = Smithing("Unable to find item")
            val OUTPUT_NOT_FOUND: Smithing get() = Smithing("Unable to find category")
            val SELECTING_BASE_MODIFIER_NOT_SUPPORTED: Smithing get() = Smithing("Base Modifier Not Supported for this Item")
            val SELECTING_BASE_MODIFIER: Smithing get() = Smithing("Selecting Base Modifier")
        }
    }

    /**
     * Production result specific to normal production activities (cooking, crafting, etc.).
     */
    class Normal(defaultMessage: String) : ProductionResult(defaultMessage) {
        companion object {
            val ITEM_NOT_FOUND: Normal get() = Normal("Unable to find item")
            val CATEGORY_NOT_FOUND: Normal get() = Normal("Unable to find category")
        }
    }
}

// Extension functions for easier creation
fun <T : ProductionResult> T.toMessage() = ProductionMessage(this)
fun <T : ProductionResult> T.toMessage(message: String) = ProductionMessage(this, message)
fun <T : ProductionResult> T.toMessage(message: String, vararg details: Pair<String, Any>) = 
    ProductionMessage(this, message, details.toMap())