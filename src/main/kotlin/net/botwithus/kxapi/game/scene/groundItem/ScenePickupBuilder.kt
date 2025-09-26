package net.botwithus.kxapi.game.scene.groundItem

import net.botwithus.xapi.script.BwuScript

/**
 * Builder class for creating ground item pickup configurations.
 * Supports fluent DSL syntax for configuring scene pickup tasks with global defaults.
 */
class ScenePickupBuilder {
    private val items = mutableListOf<ScenePickupItem>()

    private var globalNotedStatus: NotedStatus = NotedStatus.BOTH
    private var globalDistance: Int = -1
    private var globalAmount: Int = 1
    private var globalPickupInCombat: Boolean = false
    private var globalItemPriority: PickupItemPriority = PickupItemPriority.MEDIUM
    private var globalPriorityOrder: List<PickupPriority> = listOf(PickupPriority.DISTANCE, PickupPriority.QUANTITY, PickupPriority.PRICE)

    /**
     * Configure global settings that will be used as defaults for all items.
     * 
     * @param config Lambda to configure global settings
     * @return This builder instance for method chaining
     */
    fun global(config: GlobalPickupConfig.() -> Unit) = apply {
        val globalConfig = GlobalPickupConfig()
        globalConfig.config()
        this.globalNotedStatus = globalConfig.notedStatus
        this.globalDistance = globalConfig.distance
        this.globalAmount = globalConfig.amount
        this.globalPickupInCombat = globalConfig.pickupInCombat
        this.globalItemPriority = globalConfig.itemPriority
        this.globalPriorityOrder = globalConfig.priorityOrder
    }

    /**
     * Add an item to pickup by exact name.
     * 
     * @param itemName The exact name of the item to pickup
     * @param amount The amount to pickup (optional, uses global default if not specified)
     * @param notedStatus The noted status preference (optional, uses global default if not specified)
     * @param distance The maximum distance to search for the item (optional, uses global default if not specified)
     * @param pickupInCombat Whether to pickup this item while in combat (optional, uses global default if not specified)
     * @param itemPriority The priority level for this specific item (optional, uses global default if not specified)
     * @return This builder instance for method chaining
     */
    fun item(
        itemName: String,
        amount: Int? = null,
        notedStatus: NotedStatus? = null,
        distance: Int? = null,
        pickupInCombat: Boolean? = null,
        itemPriority: PickupItemPriority? = null
    ) = apply {
        items.add(ScenePickupItem(
            itemName,
            amount ?: globalAmount,
            notedStatus ?: globalNotedStatus,
            distance ?: globalDistance,
            SearchMode.NAME,
            pickupInCombat ?: globalPickupInCombat,
            itemPriority ?: globalItemPriority
        ))
    }

    /**
     * Add an item to pickup by ID.
     * 
     * @param itemId The ID of the item to pickup
     * @param amount The amount to pickup (optional, uses global default if not specified)
     * @param notedStatus The noted status preference (optional, uses global default if not specified)
     * @param distance The maximum distance to search for the item (optional, uses global default if not specified)
     * @param pickupInCombat Whether to pickup this item while in combat (optional, uses global default if not specified)
     * @param itemPriority The priority level for this specific item (optional, uses global default if not specified)
     * @return This builder instance for method chaining
     */
    fun item(
        itemId: Int,
        amount: Int? = null,
        notedStatus: NotedStatus? = null,
        distance: Int? = null,
        pickupInCombat: Boolean? = null,
        itemPriority: PickupItemPriority? = null
    ) = apply {
        items.add(ScenePickupItem(
            itemId.toString(),
            amount ?: globalAmount,
            notedStatus ?: globalNotedStatus,
            distance ?: globalDistance,
            SearchMode.ID,
            pickupInCombat ?: globalPickupInCombat,
            itemPriority ?: globalItemPriority
        ))
    }

    /**
     * Add an item to pickup using regex pattern.
     * 
     * @param regexPattern The regex pattern to match item names
     * @param amount The amount to pickup (optional, uses global default if not specified)
     * @param notedStatus The noted status preference (optional, uses global default if not specified)
     * @param distance The maximum distance to search for the item (optional, uses global default if not specified)
     * @param pickupInCombat Whether to pickup this item while in combat (optional, uses global default if not specified)
     * @param itemPriority The priority level for this specific item (optional, uses global default if not specified)
     * @return This builder instance for method chaining
     */
    fun item(
        regexPattern: Regex,
        amount: Int? = null,
        notedStatus: NotedStatus? = null,
        distance: Int? = null,
        pickupInCombat: Boolean? = null,
        itemPriority: PickupItemPriority? = null
    ) = apply {
        items.add(ScenePickupItem(
            regexPattern.pattern,
            amount ?: globalAmount,
            notedStatus ?: globalNotedStatus,
            distance ?: globalDistance,
            SearchMode.REGEX,
            pickupInCombat ?: globalPickupInCombat,
            itemPriority ?: globalItemPriority
        ))
    }

    /**
     * Add multiple items at once.
     *
     * @param items Vararg of ScenePickupItem instances
     * @return This builder instance for method chaining
     */
    fun items(vararg items: ScenePickupItem) = apply {
        this.items.addAll(items)
    }

    /**
     * Build the GroundLooting with the configured items.
     * 
     * @param script The BwuScript instance to use
     * @return Configured GroundLooting
     * @throws IllegalStateException if no items are configured
     */
    fun build(script: BwuScript): GroundItemPickup {
        require(items.isNotEmpty()) { "At least one item must be configured" }
        return GroundItemPickup(script, items.toList(), globalPriorityOrder)
    }

    class GlobalPickupConfig {
        var notedStatus: NotedStatus = NotedStatus.BOTH
        var distance: Int = 10
        var amount: Int = 1
        var pickupInCombat: Boolean = false
        var itemPriority: PickupItemPriority = PickupItemPriority.MEDIUM
        var priorityOrder: List<PickupPriority> = listOf(PickupPriority.DISTANCE, PickupPriority.QUANTITY, PickupPriority.PRICE)
        
        /**
         * Set the global noted status preference.
         */
        fun stackAllow(status: NotedStatus) = apply { this.notedStatus = status }

        /**
         * Set the global maximum distance for item pickup.
         */
        fun distance(distance: Int) = apply { this.distance = distance }

        /**
         * Set the global amount for item pickup.
         */
        fun amount(amount: Int) = apply { this.amount = amount }

        /**
         * Set the global pickup in combat setting.
         */
        fun pickupInCombat(enabled: Boolean) = apply { this.pickupInCombat = enabled }

        /**
         * Set the global item priority for all items.
         */
        fun itemPriority(priority: PickupItemPriority) = apply { 
            this.itemPriority = priority 
        }

        /**
         * Set the priority order for sorting pickup items.
         * Items will be sorted by the first priority, then by the second if equal, etc.
         */
        fun priorityOrder(vararg priorities: PickupPriority) = apply { 
            this.priorityOrder = priorities.toList() 
        }
    }

}
