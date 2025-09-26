package net.botwithus.kxapi.game.scene

import net.botwithus.kxapi.game.scene.groundItem.GroundItemPickup
import net.botwithus.kxapi.game.scene.groundItem.ScenePickupBuilder
import net.botwithus.xapi.script.BwuScript

/**
 * DSL context for scene operations.
 * Provides a structured namespace for scene-related operations like ground item pickup.
 * 
 * @param script The BwuScript instance to use for operations
 */
class Scene(private val script: BwuScript) {

    /**
     * Create and configure a ground item pickup task.
     *
     * @param builder Lambda to configure the pickup task
     * @return Configured GroundItemPickup instance
     */
    fun pickup(builder: ScenePickupBuilder.() -> Unit): GroundItemPickup {
        return ScenePickupBuilder().apply(builder).build(script)
    }
}

val BwuScript.scene: Scene
    get() = Scene(this)
