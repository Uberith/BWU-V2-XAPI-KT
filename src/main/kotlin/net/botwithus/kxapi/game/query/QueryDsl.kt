package net.botwithus.kxapi.game.query

import net.botwithus.xapi.query.ComponentQuery
import net.botwithus.xapi.query.GroundItemQuery
import net.botwithus.xapi.query.InventoryItemQuery
import net.botwithus.xapi.query.NpcQuery
import net.botwithus.xapi.query.SceneObjectQuery

/**
 * Lightweight DSL entry points that mirror the Java helpers while keeping
 * the Kotlin call-site terse.
 */
inline fun sceneObjects(block: SceneObjectQuery.() -> Unit): SceneObjectQuery =
    SceneObjectQuery.newQuery().apply(block)

inline fun groundItems(block: GroundItemQuery.() -> Unit): GroundItemQuery =
    GroundItemQuery.newQuery().apply(block)

inline fun npcs(block: NpcQuery.() -> Unit): NpcQuery =
    NpcQuery.newQuery().apply(block)

inline fun inventoryItems(vararg inventoryIds: Int, block: InventoryItemQuery.() -> Unit): InventoryItemQuery =
    InventoryItemQuery.newQuery(*inventoryIds).apply(block)

inline fun components(vararg interfaceIds: Int, block: ComponentQuery.() -> Unit): ComponentQuery =
    ComponentQuery.newQuery(*interfaceIds).apply(block)
