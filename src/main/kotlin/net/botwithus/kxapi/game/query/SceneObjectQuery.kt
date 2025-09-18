package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.rs3.cache.assets.so.SceneObjectDefinition
import net.botwithus.rs3.entities.SceneObject
import net.botwithus.rs3.world.World
import java.util.Arrays
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * SceneObject query aligned with XAPI-style queries.
 * - Backed by World.getSceneObjects()
 * - Returns ResultSet<SceneObject> so extensions like ResultSet.nearest() work
 */
class SceneObjectQuery : Query<SceneObject> {

    private var root: Predicate<SceneObject> = Predicate { true }

    companion object {
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

    /** Filters by one or more type ids. No-op when empty. */
    fun typeId(vararg typeIds: Int): SceneObjectQuery {
        if (typeIds.isEmpty()) return this
        val set = typeIds.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && set.contains(t.typeId) }
        return this
    }

    /** Filters by one or more animation ids. No-op when empty. */
    fun animation(vararg animations: Int): SceneObjectQuery {
        if (animations.isEmpty()) return this
        val set = animations.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && set.contains(t.animationId) }
        return this
    }

    /** Filters by hidden flag. Use hidden(false) for visible objects. */
    fun hidden(hidden: Boolean): SceneObjectQuery {
        val prev = root
        root = Predicate { t -> prev.test(t) && t.isHidden == hidden }
        return this
    }

    /** Filters by multi-type (definition). No-op when empty. */
    fun multiType(vararg sceneObjectDefinitions: SceneObjectDefinition): SceneObjectQuery {
        if (sceneObjectDefinitions.isEmpty()) return this
        val defs = sceneObjectDefinitions.toSet()
        val prev = root
        root = Predicate { t -> prev.test(t) && defs.contains(t.multiType) }
        return this
    }

    /** Filters by name using a custom predicate. No-op when empty. */
    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): SceneObjectQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objName = t.name
            prev.test(t) && objName != null && names.any { n -> spred.apply(n, objName) }
        }
        return this
    }

    /** Filters by exact name using String.contentEquals semantics. */
    fun name(vararg names: String): SceneObjectQuery = name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    /** Filters by regex Patterns (full match). No-op when empty. */
    fun name(vararg patterns: Pattern): SceneObjectQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val objName = t.name
            prev.test(t) && objName != null && Arrays.stream(patterns).anyMatch { p -> p.matcher(objName).matches() }
        }
        return this
    }

    /** Filters by options using a custom predicate. No-op when empty. */
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

    /** Filters by options using String.contentEquals semantics. */
    fun option(vararg option: String): SceneObjectQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *option)

    /** Filters by options using regex Patterns. No-op when empty. */
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