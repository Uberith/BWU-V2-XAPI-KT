package net.botwithus.kxapi.game.query.base

import java.time.Duration
import java.util.Collections
import java.util.WeakHashMap
import net.botwithus.kxapi.game.query.result.ResultSet

private data class CachedResult(val expiryNanos: Long, val results: ResultSet<*>)

private val queryCache = Collections.synchronizedMap(WeakHashMap<Query<*>, CachedResult>())

/**
 * Materialises the query while caching the snapshot for [ttl]. Subsequent calls within
 * the time window return the cached [ResultSet] to avoid repeatedly hitting the game API.
 */
fun <T> Query<T>.resultsCached(ttl: Duration): ResultSet<T> {
    if (ttl.isZero || ttl.isNegative) return results()
    val now = System.nanoTime()
    val cached = synchronized(queryCache) {
        val entry = queryCache[this] ?: return@synchronized null
        if (now <= entry.expiryNanos) entry.results else null
    }
    if (cached != null) {
        @Suppress("UNCHECKED_CAST")
        return cached as ResultSet<T>
    }

    val fresh = results()
    val ttlNanos = try {
        ttl.toNanos()
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }
    val expiry = if (ttlNanos >= Long.MAX_VALUE - now) Long.MAX_VALUE else now + ttlNanos
    synchronized(queryCache) {
        queryCache[this] = CachedResult(expiry, fresh)
    }
    return fresh
}

/** Clears any cached snapshot associated with this query instance. */
fun <T> Query<T>.invalidateCache() {
    synchronized(queryCache) {
        queryCache.remove(this)
    }
}
