package net.botwithus.kxapi.game.scene.groundItem

import net.botwithus.kxapi.util.componentIndex
import net.botwithus.kxapi.util.inCombat
import net.botwithus.rs3.cache.assets.ConfigManager
import net.botwithus.rs3.cache.assets.items.StackType
import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.rs3.interfaces.Interfaces
import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.inventories.InventoryManager
import net.botwithus.rs3.item.GroundItem
import net.botwithus.rs3.item.Item
import net.botwithus.rs3.minimenu.Action
import net.botwithus.rs3.minimenu.MiniMenu
import net.botwithus.rs3.world.Distance
import net.botwithus.rs3.world.World
import net.botwithus.xapi.game.inventory.Backpack
import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.script.BwuScript
import java.util.Comparator


/**
 * Handles ground item pickup operations with configurable filtering and matching.
 * 
 * @param script The BwuScript instance to use for operations
 * @param items List of configured pickup items with their constraints
 * @param priorityOrder Order of priorities for sorting pickup items
 */
class GroundItemPickup(
    private val script: BwuScript, 
    private val items: List<ScenePickupItem>,
    private val priorityOrder: List<PickupPriority> = listOf(PickupPriority.DISTANCE, PickupPriority.QUANTITY, PickupPriority.PRICE)
) {

    var LOOT_INVENTORY: Inventory? = null
    
    // Cache for better performance
    private val itemProvider = ConfigManager.getItemProvider()
    private val player = LocalPlayer.self()
    
    // Cache for item priorities to avoid repeated lookups
    private val itemPriorityCache = mutableMapOf<Int, Int>()
    
    // Pre-compiled regex patterns for better performance
    private val regexCache = mutableMapOf<String, Regex>()

    /**
     * Search for ground items and attempt to match them with configured pickup items.
     * 
     * @return pickup indicating the search result
     */
    fun pickup(): PickupMessages {
        if (LOOT_INVENTORY == null) {
            LOOT_INVENTORY = InventoryManager.getInventory(773)
        }

        // Use sequence for lazy evaluation and better performance
        val groundItems = World.getGroundItems()
            .asSequence()
            .filter { it.isReachable }
            .flatMap { ground -> ground.items.asSequence() }
            .filter { item ->
                val distanceToItem = Distance.to(item.stack.coordinate).toInt()
                findMatchingPickupConfig(item, distanceToItem) != null
            }
            .sortedWith(createItemComparator())
            .toList()
        
        return if (groundItems.isNotEmpty()) handlePickup(groundItems) else PickupMessages.NO_ITEMS_FOUND
    }

    private fun handlePickup(items: List<GroundItem>): PickupMessages {
        if (Interfaces.isOpen(1622)) {
            return handleLootingInterface()
        }

        val isInCombat = player.inCombat()
        val isMoving = player.isMoving

        for (item in items) {
            val pickupConfig = findMatchingPickupConfig(item, 0)

            // Skip if in combat and not allowed
            if (isInCombat && pickupConfig?.pickupInCombat != true) {
                continue
            }
            
            // Skip if no room or player is moving
            if (!hasRoomFor(item) || isMoving) {
                continue
            }
            
            val itemCoord = item.stack.coordinate
            val state = MiniMenu.doAction(Action.GROUND_ITEM3, item.id, itemCoord.x(), itemCoord.y())
            if (state > 0 && Interfaces.isOpen(1622)) {
                return handleLootingInterface()
            }
        }

        return PickupMessages.NO_ITEMS_FOUND
    }

    private fun handleLootingInterface(): PickupMessages {
        val inventory = LOOT_INVENTORY ?: return PickupMessages.NO_ITEMS_FOUND
        if (inventory.items.isEmpty()) return PickupMessages.NO_ITEMS_FOUND

        val isInCombat = player.inCombat()

        val validItems = inventory.items
            .asSequence()
            .mapNotNull { item ->
                val itemID = item.id
                val itemName = item.name
                val isNoted = itemProvider.provide(itemID).isNote
                val pickupConfig = findMatchingPickupConfig(itemID, itemName, isNoted, -1)
                
                if (pickupConfig != null) {
                    Triple(item, pickupConfig, isNoted)
                } else null
            }
            .filter { (_, pickupConfig, _) ->
                !isInCombat || pickupConfig.pickupInCombat
            }
            .filter { (item, _, isNoted) ->
                val stackType = if (isNoted) StackType.ALWAYS else StackType.NEVER
                hasRoomFor(item.id, item.quantity, stackType)
            }
            .sortedWith(Comparator { a, b ->
                val (itemA, _, _) = a
                val (itemB, _, _) = b
                createItemComparator().compare(itemA, itemB)
            })
            .toList().sortedByDescending { it.second.pickupInCombat }

        validItems.forEach { (item, _, _) ->
            val clicked = ComponentQuery.newQuery(1622)
                .componentIndex(11)
                .flatMap { it.children.withIndex() }
                .firstOrNull { (index, child) -> index == item.slot && child.itemId != -1 }
                ?.value?.interact()

        }

        return PickupMessages.LOOTING_INTERFACE
    }

    private fun hasRoomFor(item: Item): Boolean {
        return hasRoomFor(item.id,item.quantity,item.stackType)
    }

    private fun hasRoomFor(id: Int, quantity: Int, stack: StackType): Boolean {
        val inventory = Backpack.getInventory()

        return when (stack) {
            StackType.ALWAYS -> {
                val canStack = Backpack.getItems().any { item ->
                    item?.id == id && item.quantity.toLong() + quantity <= Int.MAX_VALUE
                }
                canStack || inventory.freeSlots() > 0
            }
            else -> inventory.freeSlots() >= quantity
        }
    }


    /**
     * Find the matching pickup configuration for a ground item.
     *
     * @param item The ground item to match
     * @param distance The distance to the item
     * @return The matching ScenePickupItem configuration or null if no match
     */
    private fun findMatchingPickupConfig(item: GroundItem, distance: Int): ScenePickupItem? {
        return findMatchingPickupConfig(item.id, item.name, item.type.isNote, distance)
    }

    /**
     * Find the matching pickup configuration using raw item data.
     *
     * @param itemId The item ID
     * @param itemName The item name
     * @param isNoted Whether the item is noted
     * @param distance The distance to the item
     * @return The matching ScenePickupItem configuration or null if no match
     */
    private fun findMatchingPickupConfig(itemId: Int, itemName: String, isNoted: Boolean, distance: Int): ScenePickupItem? {
        return items.firstOrNull { pickupItem ->
            matchesDistanceConstraint(distance, pickupItem.distance) &&
            matchesNotedStatusConstraint(isNoted, pickupItem.notedStatus) &&
            matchesItemName(itemId, itemName, pickupItem)
        }
    }

    private fun matchesDistanceConstraint(itemDistance: Int, maxDistance: Int): Boolean {
        return maxDistance == -1 || itemDistance <= maxDistance
    }

    /**
     * Check if the item's noted status matches the pickup configuration.
     */
    private fun matchesNotedStatusConstraint(isNoted: Boolean, requiredStatus: NotedStatus): Boolean {
        return when (requiredStatus) {
            NotedStatus.UNNOTED -> !isNoted
            NotedStatus.NOTED_ONLY -> isNoted
            NotedStatus.BOTH -> true
        }
    }

    /**
     * Check if the item name matches the pickup configuration based on search mode using raw data.
     */
    private fun matchesItemName(itemId: Int, itemName: String, pickupItem: ScenePickupItem): Boolean {
        return when (pickupItem.searchMode) {
            SearchMode.NAME -> itemName.equals(pickupItem.itemName, ignoreCase = true)
            SearchMode.ID -> itemId == pickupItem.itemName.toInt()
            SearchMode.REGEX -> {
                // Use cached regex for better performance
                val regex = regexCache.getOrPut(pickupItem.itemName) { pickupItem.itemName.toRegex() }
                itemName.matches(regex)
            }
        }
    }

    /**
     * Create a comparator that applies item priority FIRST, then main sorting within each priority group.
     * This ensures that high priority items are picked up first regardless of distance/quantity/price.
     */
    private fun createItemComparator(): Comparator<Item> {
        return Comparator { a, b ->
            val priorityA = getItemPriority(a)
            val priorityB = getItemPriority(b)
            val priorityComparison = priorityA.ordinal.compareTo(priorityB.ordinal)
            
            if (priorityComparison != 0) {
                priorityComparison
            } else {
                priorityOrder.firstNotNullOfOrNull { priority ->
                    compareByPriority(priority, a, b).takeIf { it != 0 }
                } ?: 0
            }
        }
    }

    /**
     * Get the item priority for a given item by finding its matching pickup configuration.
     */
    private fun getItemPriority(item: Item): PickupItemPriority {
        val pickupConfig = when (item) {
            is GroundItem -> findMatchingPickupConfig(item, 0)
            else -> {
                val itemProvider = ConfigManager.getItemProvider()
                val isNoted = itemProvider.provide(item.id).isNote
                findMatchingPickupConfig(item.id, item.name, isNoted, -1)
            }
        }
        return pickupConfig?.itemPriority ?: PickupItemPriority.MEDIUM
    }

    /**
     * Compare two items by a specific priority.
     */
    private fun compareByPriority(priority: PickupPriority, item1: Item, item2: Item): Int = when (priority) {
        PickupPriority.DISTANCE -> {
            if (item1 is GroundItem && item2 is GroundItem) {
                val distance1 = Distance.to(item2.stack.coordinate).toInt()
                val distance2 = Distance.to(item1.stack.coordinate).toInt()
                distance1.compareTo(distance2)
            } else 0
        }
        PickupPriority.QUANTITY -> item1.quantity.compareTo(item2.quantity)
        PickupPriority.PRICE -> {
            val price1 = getItemPrice(item1.id)
            val price2 = getItemPrice(item2.id)
            price2.compareTo(price1)
        }
    }


    /**
     * Get the price of an item (placeholder implementation).
     * You may need to implement this based on your price data source.
     */
    private fun getItemPrice(itemId: Int): Int {
        return 0
    }

}