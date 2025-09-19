package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.result.nearest
import net.botwithus.kxapi.util.StringMatchers
import net.botwithus.rs3.cache.assets.so.SceneObjectDefinition
import net.botwithus.rs3.entities.SceneObject
import net.botwithus.rs3.world.Distance
import net.botwithus.rs3.world.World
import java.util.Arrays
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Fluent query builder for [SceneObject] instances backed by [World.getSceneObjects].
 *
 * Each filtering call appends to the internal predicate, so you can keep chaining
 * until you are ready to materialise the result set. The most common pattern is:
 *
 * ```kotlin
 * val altar = SceneObjectQuery.newQuery()
 *     .withName("Chaos altar")
 *     .hidden(false)
 *     .within(8.0)
 *     .nearestOrNull()
 * ```
 *
 * You can also iterate the query directly: `for (obj in SceneObjectQuery.newQuery().typeId(1234)) { ... }`.
 */
class SceneObjectQuery : Query<SceneObject> {

    private var root: Predicate<SceneObject> = Predicate { true }

    companion object {
        /** Convenience factory that mirrors the Java-facing `SceneObjectQuery.newQuery()` helper. */
        @JvmStatic
        fun newQuery(): SceneObjectQuery = SceneObjectQuery()
    }

    override fun results(): ResultSet<SceneObject> {
        val all = World.getSceneObjects()
        val filtered = all.filter { so -> root.test(so) }
        return ResultSet(filtered)
    }

    override fun iterator(): MutableIterator<SceneObject> = results().iterator()

    override fun test(sceneObject: SceneObject): Boolean = root.test(sceneObject)

    /**
     * Restricts the query to objects whose `typeId` matches any of the provided ids.
     * Use it when you know the definition id rather than the display name.
     */
    fun typeId(vararg typeIds: Int): SceneObjectQuery {
        if (typeIds.isEmpty()) return this
        val set = typeIds.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && set.contains(t.typeId) }
        return this
    }

    /**
     * Filters scene objects by their current animation ids. Useful for detecting
     * state changes (e.g. whether a door is currently animating).
     */
    fun animation(vararg animations: Int): SceneObjectQuery {
        if (animations.isEmpty()) return this
        val set = animations.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && set.contains(t.animationId) }
        return this
    }

    /**
     * Matches objects whose visibility aligns with [hidden]. Passing `false` keeps
     * only visible objects, whereas `true` selects entities hidden from the scene.
     */
    fun hidden(hidden: Boolean): SceneObjectQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && t.isHidden == hidden }
        return this
    }

    /**
     * Limits the query to objects backed by one of the supplied multi-type definitions.
     * Combine it with [typeId] when you need to honour multi-variant objects.
     */
    fun multiType(vararg sceneObjectDefinitions: SceneObjectDefinition): SceneObjectQuery {
        if (sceneObjectDefinitions.isEmpty()) return this
        val defs = sceneObjectDefinitions.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && defs.contains(t.multiType) }
        return this
    }

    /** Constrains the results to scene objects within [distance] tiles of the local player. */
    infix fun within(distance: Double): SceneObjectQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && Distance.to(t) <= distance }
        return this
    }

    /**
     * Applies a custom string comparison against each object name. Supply the same
     * `BiFunction` that the Java XAPI exposes (e.g. case-insensitive matchers).
     * When [names] is empty the call is ignored.
     */
    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): SceneObjectQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objName = t.name
            prev.test(t) && objName != null && names.any { n -> spred.apply(n, objName) }
        }
        return this
    }

    /**
     * Convenience overload that matches objects whose displayed name equals any of
     * the provided [names] (case-sensitive).
     */
    fun name(vararg names: String): SceneObjectQuery = name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    /** Case-insensitive exact name comparison. */
    fun nameEqualsIgnoreCase(vararg names: String): SceneObjectQuery = name(StringMatchers.equalsIgnoreCase, *names)

    /** Keeps objects whose name contains any of the [fragments] (case-sensitive). */
    fun nameContains(vararg fragments: String): SceneObjectQuery = name(StringMatchers.contains, *fragments)

    /** Case-insensitive containment check for object names. */
    fun nameContainsIgnoreCase(vararg fragments: String): SceneObjectQuery = name(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Filters by name using regular expressions. Each pattern must fully match the
     * object name; use `.*` wildcards if you only need a partial match.
     */
    fun name(vararg patterns: Pattern): SceneObjectQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objName = t.name
            prev.test(t) && objName != null && Arrays.stream(patterns).anyMatch { p -> p.matcher(objName).matches() }
        }
        return this
    }

    /**
     * Applies a custom comparison against every interaction option shown on the
     * object. Handy for fuzzy matches such as startsWith or case-insensitive checks.
     */
    fun option(spred: BiFunction<String, CharSequence, Boolean>, vararg options: String): SceneObjectQuery {
        if (options.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objOptions = t.options
            prev.test(t) && objOptions != null && objOptions.isNotEmpty() && options.any { i ->
                objOptions.any { j -> j != null && spred.apply(i, j) }
            }
        }
        return this
    }

    /** Matches objects containing any of the provided option strings (case-sensitive). */
    fun option(vararg option: String): SceneObjectQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *option)

    /** Keeps objects exposing an option that equals any value, ignoring case differences. */
    fun optionEqualsIgnoreCase(vararg options: String): SceneObjectQuery = option(StringMatchers.equalsIgnoreCase, *options)

    /** Filters objects whose options contain any fragment (case-sensitive). */
    fun optionContains(vararg fragments: String): SceneObjectQuery = option(StringMatchers.contains, *fragments)

    /** Case-insensitive containment for option strings. */
    fun optionContainsIgnoreCase(vararg fragments: String): SceneObjectQuery = option(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Keeps objects that expose an option matching any of the supplied regular expressions.
     * The regex must match the entire option string.
     */
    fun option(vararg patterns: Pattern): SceneObjectQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objOptions = t.options
            prev.test(t) && objOptions != null && objOptions.any { opt ->
                opt != null && Arrays.stream(patterns).anyMatch { p -> p.matcher(opt).matches() }
            }
        }
        return this
    }
}

/** Materialises the query and returns the nearest matching scene object, or null. */
fun SceneObjectQuery.nearestOrNull(): SceneObject? = results().nearest()

/** Fluent alias so chains read as `query withName "Banker"`. */
infix fun SceneObjectQuery.withName(name: String): SceneObjectQuery = this.name(name)

/** Alias for [SceneObjectQuery.option] that reads as `query withOption "Chop down"`. */
infix fun SceneObjectQuery.withOption(option: String): SceneObjectQuery = this.option(option)

