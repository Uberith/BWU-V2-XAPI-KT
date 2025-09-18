package net.botwithus.kxapi.game.query.base

import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.result.nearestBy


/**
 * Base contract for all fluent query builders exposed by the XAPI.
 *
 * A query implementation accumulates predicate state so callers can chain filters
 * and defer materialisation until [results] or iteration is requested. The
 * returned [ResultSet] keeps the lazy helpers (e.g. `nearest()`, `firstOrNull()`)
 * that scripts rely on when interacting with the game world.
 *
 * Typical usage looks like:
 * ```kotlin
 * val objects = SceneObjectQuery.newQuery()
 *     .withName("Bank booth")
 *     .firstMatching()
 * ```
 *
 * Queries are also [MutableIterable], so they can be used directly in a
 * `for` loop which implicitly materialises [results]. Convenience extensions
 * such as [firstMatching] and `nearestOrNull()` lean on this behaviour.
 */
interface Query<T> : MutableIterable<T> {

    /**
     * Materialises the query by fetching the backing collection and applying
     * every chained predicate. The snapshot is wrapped inside a [ResultSet]
     * so callers can keep chaining helper operations provided by the runtime.
     */
    fun results(): ResultSet<T>

    /**
     * Runs the aggregated predicate against a single value without triggering
     * a new materialisation. This is useful when you already hold a reference
     * and only need to verify whether it still passes the query filters.
     */
    fun test(t: T): Boolean
}

/** Materialises the query and returns the first element, or `null` when empty. */
fun <T> Query<T>.firstMatching(): T? = results().firstOrNull()

/** Finds the first element that satisfies [predicate], materialising at most once. */
inline fun <T> Query<T>.firstMatching(predicate: (T) -> Boolean): T? {
    for (candidate in this) {
        if (predicate(candidate)) return candidate
    }
    return null
}


/** Computes the nearest element according to [distanceProvider], or null when empty. */
fun <T> Query<T>.nearestOrNull(distanceProvider: (T) -> Double): T? = results().nearestBy(distanceProvider)
