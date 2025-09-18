package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
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
 * Kotlin-friendly ground item query mirroring the BotWithUs XAPI surface.
 *
 * It fetches items from [World.getGroundItems] and exposes fluent filters while
 * returning the shared [ResultSet] type used throughout the script API.
 */
class GroundItemQuery private constructor() : Query<GroundItem> {

    private var root: Predicate<GroundItem> = Predicate { true }

    companion object {
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

    fun id(vararg ids: Int): GroundItemQuery {
        if (ids.isEmpty()) return this
        val set = ids.toSet()
        root = root.and { set.contains(it.id) }
        return this
    }

    fun quantity(spred: BiFunction<Int, Int, Boolean>, quantity: Int): GroundItemQuery {
        root = root.and { spred.apply(it.quantity, quantity) }
        return this
    }

    fun quantity(quantity: Int): GroundItemQuery = quantity(BiFunction { a, b -> a == b }, quantity)

    fun itemTypes(vararg itemTypes: ItemDefinition): GroundItemQuery {
        if (itemTypes.isEmpty()) return this
        val set = itemTypes.toSet()
        root = root.and { set.contains(it.type) }
        return this
    }

    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): GroundItemQuery {
        if (names.isEmpty()) return this
        root = root.and { item ->
            val itemName = item.name
            itemName != null && names.any { spred.apply(it, itemName) }
        }
        return this
    }

    fun name(vararg names: String): GroundItemQuery =
        name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    fun name(vararg patterns: Pattern): GroundItemQuery {
        if (patterns.isEmpty()) return this
        root = root.and { item ->
            val itemName = item.name
            itemName != null && patterns.any { it.matcher(itemName).matches() }
        }
        return this
    }

    fun stackType(vararg stackTypes: StackType): GroundItemQuery {
        if (stackTypes.isEmpty()) return this
        val set = stackTypes.toSet()
        root = root.and { set.contains(it.stackType) }
        return this
    }

    fun coordinate(vararg coordinates: Coordinate): GroundItemQuery {
        if (coordinates.isEmpty()) return this
        val set = coordinates.toSet()
        root = root.and { set.contains(it.stack.coordinate) }
        return this
    }

    fun inside(area: Area): GroundItemQuery {
        root = root.and { area.contains(it.stack.coordinate) }
        return this
    }

    fun outside(area: Area): GroundItemQuery {
        root = root.and { !area.contains(it.stack.coordinate) }
        return this
    }

    fun distance(distance: Double): GroundItemQuery {
        root = root.and { Distance.to(it.stack.coordinate) <= distance }
        return this
    }

    fun valid(valid: Boolean): GroundItemQuery {
        root = root.and { it.stack.isValid == valid }
        return this
    }

    fun and(other: GroundItemQuery): GroundItemQuery {
        root = root.and(other.root)
        return this
    }

    fun or(other: GroundItemQuery): GroundItemQuery {
        root = root.or(other.root)
        return this
    }

    fun invert(): GroundItemQuery {
        root = root.negate()
        return this
    }

    fun mark(): GroundItemQuery = this
}
