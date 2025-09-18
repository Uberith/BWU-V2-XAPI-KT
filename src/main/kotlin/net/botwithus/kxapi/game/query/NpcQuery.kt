package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.result.nearest
import net.botwithus.kxapi.game.query.util.StringMatchers
import net.botwithus.rs3.entities.PathingEntity
import net.botwithus.rs3.world.Distance
import net.botwithus.rs3.world.World
import java.util.Arrays
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Query wrapper for NPC [PathingEntity] instances exposed by [World.getNpcs].
 *
 * The builder mirrors the original XAPI surface so scripts can fluently express
 * filters and still consume the shared [ResultSet] helpers.
 */
class NpcQuery : Query<PathingEntity> {

    private var root: Predicate<PathingEntity> = Predicate { true }

    companion object {
        /** Factory to align with the Java DSL (`NpcQuery.newQuery()`). */
        @JvmStatic
        fun newQuery(): NpcQuery = NpcQuery()
    }

    override fun results(): ResultSet<PathingEntity> {
        val all = try { World.getNpcs() } catch (_: Throwable) { emptyList<PathingEntity>() }
        val filtered = all.filter { n -> root.test(n) }
        return ResultSet(filtered)
    }

    override fun iterator(): MutableIterator<PathingEntity> = results().iterator()

    override fun test(t: PathingEntity): Boolean = root.test(t)

    /** Restricts the query to NPCs whose `typeId` appears in [ids]. */
    fun typeId(vararg ids: Int): NpcQuery {
        if (ids.isEmpty()) return this
        val prev = root
        val set = ids.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.typeId) }
        return this
    }

    /** Constrains the results to NPCs within [distance] tiles of the local player. */
    infix fun within(distance: Double): NpcQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && Distance.to(t) <= distance }
        return this
    }

    /**
     * Applies a custom comparator against the NPC name. Handy for case folding or
     * fuzzy matches (e.g. `startsWith`). The call is ignored when [names] is empty.
     */
    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): NpcQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val nm = t.name
            prev.test(t) && nm != null && names.any { n -> spred.apply(n, nm) }
        }
        return this
    }

    /** Filters NPCs whose name matches any provided value exactly (case-sensitive). */
    fun name(vararg names: String): NpcQuery = name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    /** Case-insensitive equality helper for NPC names. */
    fun nameEqualsIgnoreCase(vararg names: String): NpcQuery = name(StringMatchers.equalsIgnoreCase, *names)

    /** Keeps NPCs whose name contains any [fragments]. */
    fun nameContains(vararg fragments: String): NpcQuery = name(StringMatchers.contains, *fragments)

    /** Case-insensitive containment helper for NPC names. */
    fun nameContainsIgnoreCase(vararg fragments: String): NpcQuery = name(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Uses regular expressions to match NPC names. Each pattern must match the
     * full string unless you add `.*` wildcards.
     */
    fun name(vararg patterns: Pattern): NpcQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val nm = t.name
            prev.test(t) && nm != null && Arrays.stream(patterns).anyMatch { p -> p.matcher(nm).matches() }
        }
        return this
    }

    /**
     * Evaluates the interaction options exposed by the NPC using a custom comparator.
     * Combine with case-insensitive logic to support localisation differences.
     */
    fun option(spred: BiFunction<String, CharSequence, Boolean>, vararg options: String): NpcQuery {
        if (options.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val opts = t.options
            prev.test(t) && opts != null && opts.isNotEmpty() && options.any { i ->
                opts.any { j -> j != null && spred.apply(i, j) }
            }
        }
        return this
    }

    /** Matches NPCs that expose any of the supplied option strings verbatim. */
    fun option(vararg options: String): NpcQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *options)

    /** Case-insensitive option equality helper. */
    fun optionEqualsIgnoreCase(vararg options: String): NpcQuery = option(StringMatchers.equalsIgnoreCase, *options)

    /** Keeps NPCs exposing options that contain any of the supplied fragments. */
    fun optionContains(vararg fragments: String): NpcQuery = option(StringMatchers.contains, *fragments)

    /** Case-insensitive containment helper for NPC options. */
    fun optionContainsIgnoreCase(vararg fragments: String): NpcQuery = option(StringMatchers.containsIgnoreCase, *fragments)
}

/** Materialises the query and returns the nearest matching NPC, or null. */
fun NpcQuery.nearestOrNull(): PathingEntity? = results().nearest()

/** Fluent alias so chains read as `query withName "Banker"`. */
infix fun NpcQuery.withName(name: String): NpcQuery = this.name(name)

/** Alias for [NpcQuery.option] expressed as `query withOption "Talk-to"`. */
infix fun NpcQuery.withOption(option: String): NpcQuery = this.option(option)

