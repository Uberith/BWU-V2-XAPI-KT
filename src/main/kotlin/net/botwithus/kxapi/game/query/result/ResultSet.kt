package net.botwithus.kxapi.game.query.result

import java.util.stream.Stream
import net.botwithus.rs3.entities.PathingEntity
import net.botwithus.rs3.entities.SceneObject
import net.botwithus.rs3.world.Distance
import java.util.Collection

class ResultSet<T>(private val data: List<T>) : MutableIterable<T> {
    fun first(): T? = data.firstOrNull()
    fun firstOrNull(): T? = data.firstOrNull()
    fun size(): Int = data.size
    fun isEmpty(): Boolean = data.isEmpty()
    fun stream(): Stream<T> = (data as Collection<T>).stream()
    fun toList(): List<T> = data
    override fun iterator(): MutableIterator<T> = data.toMutableList().iterator()
}

fun <T> ResultSet<T>.nearestBy(distanceProvider: (T) -> Double): T? {
    var nearest: T? = null
    var bestDistance = Double.MAX_VALUE
    for (item in this) {
        val distance = try {
            distanceProvider(item)
        } catch (_: Throwable) {
            continue
        }
        if (distance < bestDistance) {
            bestDistance = distance
            nearest = item
        }
    }
    return nearest
}

fun ResultSet<SceneObject>.nearest(): SceneObject? = nearestBy { Distance.to(it) }

fun ResultSet<PathingEntity>.nearest(): PathingEntity? = nearestBy { Distance.to(it) }

