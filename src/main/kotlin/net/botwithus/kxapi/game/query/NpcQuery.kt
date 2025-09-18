package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.rs3.entities.PathingEntity
import net.botwithus.rs3.world.World
import java.util.Arrays
import java.util.function.BiFunction
import java.util.function.Predicate
import java.util.regex.Pattern

class NpcQuery : Query<PathingEntity> {

    private var root: Predicate<PathingEntity> = Predicate { true }

    companion object {
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

    fun typeId(vararg ids: Int): NpcQuery {
        if (ids.isEmpty()) return this
        val prev = root
        val set = ids.toSet()
        root = Predicate { t -> prev.test(t) && set.contains(t.typeId) }
        return this
    }

    fun name(spred: BiFunction<String, CharSequence, Boolean>, vararg names: String): NpcQuery {
        if (names.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val nm = t.name
            prev.test(t) && nm != null && names.any { n -> spred.apply(n, nm) }
        }
        return this
    }

    fun name(vararg names: String): NpcQuery = name(BiFunction { a, b -> a.contentEquals(b) }, *names)

    fun name(vararg patterns: Pattern): NpcQuery {
        if (patterns.isEmpty()) return this
        val prev = root
        root = Predicate { t ->
            val nm = t.name
            prev.test(t) && nm != null && Arrays.stream(patterns).anyMatch { p -> p.matcher(nm).matches() }
        }
        return this
    }

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

    fun option(vararg options: String): NpcQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *options)
}