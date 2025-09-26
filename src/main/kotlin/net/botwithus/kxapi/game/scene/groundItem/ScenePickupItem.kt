package net.botwithus.kxapi.game.scene.groundItem

/**
 * Data class representing an item to pickup from the scene.
 *
 * @param itemName The name/pattern/ID of the item (interpreted based on searchMode)
 * @param amount The amount to pickup (optional, defaults to 1)
 * @param notedStatus The noted status preference (optional, defaults to UNNOTED)
 * @param distance The maximum distance to search for the item (optional, defaults to 5)
 * @param searchMode The search mode to use for item identification (optional, defaults to NAME)
 * @param pickupInCombat Whether to pickup this item while in combat (optional, defaults to false)
 * @param itemPriority The priority level for this specific item (optional, defaults to MEDIUM)
 */
data class ScenePickupItem(
    val itemName: String,
    val amount: Int = 1,
    val notedStatus: NotedStatus,
    val distance: Int,
    val searchMode: SearchMode,
    val pickupInCombat: Boolean = false,
    val itemPriority: PickupItemPriority = PickupItemPriority.MEDIUM
)

enum class NotedStatus {
    /** Pick up only unnoted items */
    UNNOTED,
    /** Pick up only noted items */
    NOTED_ONLY,
    /** Pick up both noted and unnoted items */
    BOTH
}

/**
 * Enum representing the priority level for individual pickup items.
 * This priority is applied after the main sorting criteria (distance, quantity, price).
 */
enum class PickupItemPriority {
    /** Very low priority - picked up last */
    VERY_LOW,
    /** Low priority */
    LOW,
    /** Medium priority - default */
    MEDIUM,
    /** High priority */
    HIGH,
    /** Very high priority - picked up first */
    VERY_HIGH
}

enum class PickupMessages {
    LOOTING_INTERFACE,
    PICKING_UP,
    FULL_INVENTORY,
    NO_ITEMS_FOUND
}

/**
 * Enum representing different priority criteria for sorting pickup items.
 * Used to determine the order in which items should be picked up.
 */
enum class PickupPriority {
    /** Sort by distance to the item (closer items first) */
    DISTANCE,
    /** Sort by quantity of the item (higher quantities first) */
    QUANTITY,
    /** Sort by item price/value (higher value items first) */
    PRICE
}

/**
 * Internal enum representing different search modes for item identification.
 * This enum is used internally by the pickup system to determine how to match items.
 *
 * The search modes are automatically determined based on the input type:
 * - String inputs use NAME mode for exact name matching
 * - Int inputs use ID mode for exact ID matching
 * - Regex inputs use REGEX mode for pattern matching
 */
enum class SearchMode {
    REGEX, ID, NAME
}
