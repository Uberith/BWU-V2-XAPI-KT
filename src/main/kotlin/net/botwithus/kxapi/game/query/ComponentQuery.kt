package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.rs3.cache.assets.ConfigManager
import net.botwithus.rs3.interfaces.Component
import net.botwithus.rs3.interfaces.ComponentType
import java.util.function.BiFunction
import java.util.function.Predicate

class ComponentQuery private constructor(private val ids: IntArray) :
    Query<Component> {

    private var root: Predicate<Component> = Predicate { t ->
        ids.any { i -> i == t.root.interfaceId }
    }

    companion object {
        @JvmStatic
        fun newQuery(vararg ids: Int): ComponentQuery = ComponentQuery(ids)
    }

    fun type(vararg type: ComponentType): ComponentQuery {
        root = root.and { t -> type.any { i -> t.type == i } }
        return this
    }

    fun id(vararg ids: Int): ComponentQuery {
        root = root.and { t -> ids.any { i -> i == t.componentId } }
        return this
    }

    fun subComponentId(vararg ids: Int): ComponentQuery {
        root = root.and { t -> ids.any { i -> i == t.subComponentId } }
        return this
    }

    fun hidden(hidden: Boolean): ComponentQuery {
        root = root.and { t -> t.isHidden == hidden }
        return this
    }

    fun properties(vararg properties: Int): ComponentQuery {
        root = root.and { t -> properties.any { i -> i == t.properties } }
        return this
    }

    fun fontId(vararg fontIds: Int): ComponentQuery {
        root = root.and { t -> fontIds.any { i -> i == t.fontId } }
        return this
    }

    fun color(vararg colors: Int): ComponentQuery {
        root = root.and { t -> colors.any { i -> i == t.color } }
        return this
    }

    fun alpha(vararg alphas: Int): ComponentQuery {
        root = root.and { t -> alphas.any { i -> i == t.alpha } }
        return this
    }

    fun itemId(vararg itemIds: Int): ComponentQuery {
        root = root.and { t -> itemIds.any { i -> i == t.itemId } }
        return this
    }

    fun itemName(name: String, spred: BiFunction<String, CharSequence, Boolean>): ComponentQuery {
        root = root.and { t ->
            val itemId = t.itemId
            val itemName = try { ConfigManager.getItemProvider().provide(itemId).name } catch (_: Throwable) { "" }
            spred.apply(name, itemName ?: "")
        }
        return this
    }

    fun itemName(name: String): ComponentQuery = itemName(name, BiFunction { a, b -> a.contentEquals(b) })

    fun itemAmount(vararg amounts: Int): ComponentQuery {
        root = root.and { t -> amounts.any { i -> i == t.itemAmount } }
        return this
    }

    fun spriteId(vararg spriteIds: Int): ComponentQuery {
        root = root.and { t -> spriteIds.any { i -> i == t.spriteId } }
        return this
    }

    fun text(spred: BiFunction<String, CharSequence, Boolean>, vararg text: String): ComponentQuery {
        root = root.and { t -> text.any { s -> spred.apply(s, t.text ?: "") } }
        return this
    }

    fun optionBasedText(spred: BiFunction<String, CharSequence, Boolean>, vararg text: String): ComponentQuery {
        root = root.and { t -> text.any { s -> spred.apply(s, t.optionBase ?: "") } }
        return this
    }

    fun option(spred: BiFunction<String, CharSequence, Boolean>, vararg option: String?): ComponentQuery {
        root = root.and { t ->
            val opts = t.options
            opts != null && option.any { o -> o != null && opts.any { j -> j != null && spred.apply(o, j) } }
        }
        return this
    }

    fun option(vararg option: String): ComponentQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *option)

    fun params(vararg params: Int): ComponentQuery {
        root = root.and { t -> params.any { p -> t.params.containsKey(p) } }
        return this
    }

    fun children(vararg ids: Int): ComponentQuery {
        root = root.and { t ->
            val kids = t.children
            kids != null && ids.any { id -> kids.any { c -> c.componentId == id } }
        }
        return this
    }

    override fun results(): ResultSet<Component> {
        val comps = mutableListOf<Component>()
        for (ifaceId in ids) {
            val list = tryGetInterfaceComponents(ifaceId)
            if (list != null) comps.addAll(list)
        }
        val filtered = comps.filter { c -> root.test(c) }
        return ResultSet(filtered)
    }

    override fun iterator(): MutableIterator<Component> = results().iterator()

    override fun test(comp: Component): Boolean = root.test(comp)

    private fun tryGetInterfaceComponents(id: Int): List<Component>? {
        return try {
            val cls = Class.forName("net.botwithus.rs3.interfaces.Interfaces")
            val m = cls.getMethod("getInterface", Int::class.javaPrimitiveType)
            val iface = m.invoke(null, id) ?: return null
            val m2 = iface.javaClass.methods.firstOrNull { it.parameterCount == 0 && it.name.lowercase().contains("component") }
            val res = m2?.invoke(iface)
            val col = when (res) {
                is Collection<*> -> res
                is Array<*> -> res.asList()
                else -> null
            } ?: return null
            col.filterIsInstance<Component>()
        } catch (_: Throwable) { null }
    }
}
