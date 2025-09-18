package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.result.nearest
import net.botwithus.kxapi.game.query.util.StringMatchers
import net.botwithus.rs3.cache.assets.items.ItemDefinition
import net.botwithus.rs3.cache.assets.items.StackType
import net.botwithus.rs3.item.GroundItem
import net.botwithus.rs3.world.Area
import net.botwithus.rs3.world.Coordinate
import net.botwithus.rs3.world.Distance
import net.botwithus.rs3.world.World
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Query builder that traverses the ground item stacks returned by [World.getGroundItems].
 *
 * Calls to the fluent filter functions accumulate and can be chained before
 * materialising results. Example usage:
 *
 * ```kotlin
 * val bones = GroundItemQuery.newQuery()
 *     .withName("Dragon bones")
 *     .within(10.0)
 *     .firstMatching()
 * ```
 *
 * Because the class implements [MutableIterable], it can also be used directly
 * in a `for` loop which invokes [results] under the hood.
 */
class GroundItemQuery private constructor() : Query<GroundItem> {

    private var root: Predicate<GroundItem> = Predicate { true }

    companion object {
        /** Factory mirroring the Java API so both Kotlin and Java callers start with `GroundItemQuery.newQuery()`. */
        @JvmStatic
        fun newQuery(): GroundItemQuery = GroundItemQuery()
    }

    override fun results(): ResultSet<GroundItem> {
        val items = World.getGroundItems().stream()
            .flatMap { it.items.stream() }
            .filter(root)
            .toList()
        return ResultSet(items)
    }

    override fun iterator(): MutableIterator<GroundItem> = results().iterator()

    override fun test(t: GroundItem): Boolean = root.test(t)

    /** Filters items whose underlying item id matches any of the provided [ids]. */
    fun id(vararg ids: Int): GroundItemQuery {
        if (ids.isEmpty()) return this
        val set = ids.toSet()
        root = root.and { set.contains(it.id) }
        return this
    }

    /**
     * Applies a custom numeric comparison to the stack quantity. Supply a predicate
     * similar to `BiFunction<Int, Int, Boolean> { current, target -> current >= target }`.
     */
    fun quantity(spred: BiFunction<Int, Int, Boolean>, quantity: Int): GroundItemQuery {
        root = root.and { spred.apply(it.quantity, quantity) }
        return this
    }

    /** Convenience overload that keeps stacks with an exact [quantity] match. */
    fun quantity(quantity: Int): GroundItemQuery = quantity(BiFunction { a, b -> a == b }, quantity)

    /** Restricts the results to items backed by any of the supplied [itemTypes]. */
    fun itemTypes(vararg itemTypes: ItemDefinition): GroundItemQuery {
        if (itemTypes.isEmpty()) return this
        val set = itemTypes.toSet()
        root = root.and { set.contains(it.type) }
        return this
    }

    /**
     * Uses a custom string comparison against the display name of each ground item.
     * Combine with case-insensitive predicates when matching human-entered strings.
     */
    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): GroundItemQuery {
        if (names.isEmpty()) return this
        root = root.and { item ->
            val itemName = item.name
            itemName != null && names.any { spred.apply(it, itemName) }
        }
        return this
    }

    /** Matches ground items whose name equals any of the provided values. */
    fun name(vararg names: String): GroundItemQuery =
        name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    /** Case-insensitive exact match against the ground item name. */
    fun nameEqualsIgnoreCase(vararg names: String): GroundItemQuery = name(StringMatchers.equalsIgnoreCase, *names)

    /** Keeps items whose name contains any of the provided fragments. */
    fun nameContains(vararg fragments: String): GroundItemQuery = name(StringMatchers.contains, *fragments)

    /** Case-insensitive containment for ground item names. */
    fun nameContainsIgnoreCase(vararg fragments: String): GroundItemQuery = name(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Filters by name using regular expressions. Every pattern must match the full
     * display name; add `.*` to support partial matches if required.
     */
    fun name(vararg patterns: Pattern): GroundItemQuery {
        if (patterns.isEmpty()) return this
        root = root.and { item ->
            val itemName = item.name
            itemName != null && patterns.any { it.matcher(itemName).matches() }
        }
        return this
    }

    /** Keeps only items whose stack type is present in [stackTypes] (e.g. notated, coin, noted). */
    fun stackType(vararg stackTypes: StackType): GroundItemQuery {
        if (stackTypes.isEmpty()) return this
        val set = stackTypes.toSet()
        root = root.and { set.contains(it.stackType) }
        return this
    }

    /** Restricts the query to exact ground coordinates. */
    fun coordinate(vararg coordinates: Coordinate): GroundItemQuery {
        if (coordinates.isEmpty()) return this
        val set = coordinates.toSet()
        root = root.and { set.contains(it.stack.coordinate) }
        return this
    }

    /** Keeps only stacks whose coordinate lies inside the supplied [area]. */
    fun inside(area: Area): GroundItemQuery {
        root = root.and { area.contains(it.stack.coordinate) }
        return this
    }

    /** Excludes any stack that falls inside [area]. */
    fun outside(area: Area): GroundItemQuery {
        root = root.and { !area.contains(it.stack.coordinate) }
        return this
    }

    /** Limits the results to stacks within [distance] tiles of the local player. */
    fun distance(distance: Double): GroundItemQuery {
        root = root.and { Distance.to(it.stack.coordinate) <= distance }
        return this
    }

    /** Infix friendly alias for [distance]. */
    infix fun within(distance: Double): GroundItemQuery = distance(distance)

    /** Chooses stacks whose validity flag matches [valid], mirroring the XAPI behaviour. */
    fun valid(valid: Boolean): GroundItemQuery {
        root = root.and { it.stack.isValid == valid }
        return this
    }

    /** Intersects this query with another one, effectively `AND`-ing both predicates. */
    fun and(other: GroundItemQuery): GroundItemQuery {
        root = root.and(other.root)
        return this
    }

    /** Combines this query with another one using logical OR semantics. */
    fun or(other: GroundItemQuery): GroundItemQuery {
        root = root.or(other.root)
        return this
    }

    /** Inverts the accumulated predicate so that matching stacks become excluded and vice versa. */
    fun invert(): GroundItemQuery {
        root = root.negate()
        return this
    }

    /** Placeholder kept for API parity; calling it simply returns the same query instance. */
    fun mark(): GroundItemQuery = this
}

/** Fluent alias so chains read as `query withName "Coins"`. */
infix fun GroundItemQuery.withName(name: String): GroundItemQuery = this.name(name)

/** Materialises the query and returns the nearest matching ground item, or null. */
fun GroundItemQuery.nearestOrNull(): GroundItem? = results().nearest()
