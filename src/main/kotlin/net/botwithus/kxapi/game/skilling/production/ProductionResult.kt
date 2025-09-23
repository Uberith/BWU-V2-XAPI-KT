package net.botwithus.kxapi.game.skilling.production

/**
 * Represents the result of production interface handling
 */
enum class ProductionResult(val message: String) {
    CREATION_IN_PROGRESS("Creation In Progress"),
    ITEM_NOT_FOUND("Unable to find item"),
    CATEGORY_NOT_FOUND("Unable to find category"),
    MISSING_REQUIREMENTS("Missing requirements"),
    INTERFACE_ERROR("Interface error")
}