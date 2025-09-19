package net.botwithus.kxapi.game.query


import net.botwithus.kxapi.game.query.base.Query
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.util.StringMatchers
import net.botwithus.rs3.cache.assets.ConfigManager
import net.botwithus.rs3.interfaces.Component
import net.botwithus.rs3.interfaces.ComponentType
import java.util.function.BiFunction
import java.util.function.Predicate

/**
 * Fluent query builder for interface [Component] instances. The query starts
 * scoped to the supplied interface ids and lets you stack additional filters
 * before materialising a [ResultSet].
 */
class ComponentQuery private constructor(private val ids: IntArray) :
    Query<Component> {

    private var root: Predicate<Component> = Predicate { t ->
        ids.any { i -> i == t.root.interfaceId }
    }

    companion object {
        /** Begins a new query restricted to the provided interface ids. */
        @JvmStatic
        fun newQuery(vararg ids: Int): ComponentQuery = ComponentQuery(ids)
    }

    /** Narrows the search to components whose [Component.type] matches any value in [type]. */
    fun type(vararg type: ComponentType): ComponentQuery {
        root = root.and { t -> type.any { i -> t.type == i } }
        return this
    }

    /** Keeps components whose `componentId` equals any of the supplied [ids]. */
    fun id(vararg ids: Int): ComponentQuery {
        root = root.and { t -> ids.any { i -> i == t.componentId } }
        return this
    }

    /** Filters by [Component.subComponentId] which is exposed on nested components. */
    fun subComponentId(vararg ids: Int): ComponentQuery {
        root = root.and { t -> ids.any { i -> i == t.subComponentId } }
        return this
    }

    /** Matches components whose [Component.isHidden] flag equals [hidden]. */
    fun hidden(hidden: Boolean): ComponentQuery {
        root = root.and { t -> t.isHidden == hidden }
        return this
    }

    /** Keeps components whose `properties` bitfield equals any of the provided [properties]. */
    fun properties(vararg properties: Int): ComponentQuery {
        root = root.and { t -> properties.any { i -> i == t.properties } }
        return this
    }

    /** Filters components by their configured font id. */
    fun fontId(vararg fontIds: Int): ComponentQuery {
        root = root.and { t -> fontIds.any { i -> i == t.fontId } }
        return this
    }

    /** Restricts the query to specific foreground colours (ARGB integer form). */
    fun color(vararg colors: Int): ComponentQuery {
        root = root.and { t -> colors.any { i -> i == t.color } }
        return this
    }

    /** Filters by alpha channel values which can signal transparency states. */
    fun alpha(vararg alphas: Int): ComponentQuery {
        root = root.and { t -> alphas.any { i -> i == t.alpha } }
        return this
    }

    /** Matches components that display an item with any of the provided [itemIds]. */
    fun itemId(vararg itemIds: Int): ComponentQuery {
        root = root.and { t -> itemIds.any { i -> i == t.itemId } }
        return this
    }

    /**
     * Executes a custom string comparison against the item name currently rendered
     * by the component. This mirrors the Java API so you can plug in case-insensitive
     * or fuzzy comparators.
     */
    fun itemName(name: String, spred: BiFunction<String, CharSequence, Boolean>): ComponentQuery {
        root = root.and { t ->
            val itemId = t.itemId
            val itemName = try { ConfigManager.getItemProvider().provide(itemId).name } catch (_: Throwable) { "" }
            spred.apply(name, itemName ?: "")
        }
        return this
    }

    /** Shorthand for matching the item name exactly. */
    fun itemName(name: String): ComponentQuery = itemName(name, BiFunction { a, b -> a.contentEquals(b) })

    /** Case-insensitive exact match helper for item names. */
    fun itemNameEqualsIgnoreCase(vararg names: String): ComponentQuery {
        root = root.and { t ->
            val itemId = t.itemId
            val itemName = try { ConfigManager.getItemProvider().provide(itemId).name } catch (_: Throwable) { "" }
            itemName != null && names.any { StringMatchers.equalsIgnoreCase(it, itemName) }
        }
        return this
    }

    /** Keeps components when the rendered item name contains any fragment. */
    fun itemNameContains(vararg fragments: String): ComponentQuery {
        root = root.and { t ->
            val itemId = t.itemId
            val itemName = try { ConfigManager.getItemProvider().provide(itemId).name } catch (_: Throwable) { "" }
            itemName != null && fragments.any { StringMatchers.contains(it, itemName) }
        }
        return this
    }

    /** Case-insensitive containment helper for rendered item names. */
    fun itemNameContainsIgnoreCase(vararg fragments: String): ComponentQuery {
        root = root.and { t ->
            val itemId = t.itemId
            val itemName = try { ConfigManager.getItemProvider().provide(itemId).name } catch (_: Throwable) { "" }
            itemName != null && fragments.any { StringMatchers.containsIgnoreCase(it, itemName) }
        }
        return this
    }

    /** Keeps components whose displayed item amount equals any of [amounts]. */
    fun itemAmount(vararg amounts: Int): ComponentQuery {
        root = root.and { t -> amounts.any { i -> i == t.itemAmount } }
        return this
    }

    /** Filters components whose sprite id matches any of [spriteIds]. */
    fun spriteId(vararg spriteIds: Int): ComponentQuery {
        root = root.and { t -> spriteIds.any { i -> i == t.spriteId } }
        return this
    }

    /**
     * Applies a string comparator against [Component.text]. Supply multiple values
     * to keep components whose text matches any of them.
     */
    fun text(spred: BiFunction<String, CharSequence, Boolean>, vararg text: String): ComponentQuery {
        root = root.and { t -> text.any { s -> spred.apply(s, t.text ?: "") } }
        return this
    }

    /** Matches text exactly against any provided string (case-sensitive). */
    fun text(vararg text: String): ComponentQuery = text(BiFunction { a, b -> a.contentEquals(b) }, *text)

    /** Convenience helper for case-insensitive text equality. */
    fun textEqualsIgnoreCase(vararg text: String): ComponentQuery = text(StringMatchers.equalsIgnoreCase, *text)

    /** Keeps components whose text contains any fragment. */
    fun textContains(vararg fragments: String): ComponentQuery = text(StringMatchers.contains, *fragments)

    /** Case-insensitive containment helper for component text. */
    fun textContainsIgnoreCase(vararg fragments: String): ComponentQuery = text(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Uses the same semantics as [text] but targets [Component.optionBase], which
     * is typically used for choice-boxes or context menus.
     */
    fun optionBasedText(spred: BiFunction<String, CharSequence, Boolean>, vararg text: String): ComponentQuery {
        root = root.and { t -> text.any { s -> spred.apply(s, t.optionBase ?: "") } }
        return this
    }

    /** Matches option base content exactly. */
    fun optionBasedText(vararg text: String): ComponentQuery = optionBasedText(BiFunction { a, b -> a.contentEquals(b) }, *text)

    /** Case-insensitive equality helper for option base text. */
    fun optionBasedTextEqualsIgnoreCase(vararg text: String): ComponentQuery = optionBasedText(StringMatchers.equalsIgnoreCase, *text)

    /** Partial match helper for option base text. */
    fun optionBasedTextContains(vararg fragments: String): ComponentQuery = optionBasedText(StringMatchers.contains, *fragments)

    /** Case-insensitive containment helper for option base text. */
    fun optionBasedTextContainsIgnoreCase(vararg fragments: String): ComponentQuery = optionBasedText(StringMatchers.containsIgnoreCase, *fragments)

    /**
     * Evaluates the interaction options exposed by the component using the provided
     * comparator. Null options are ignored to mirror the behaviour of the Java API.
     */
    fun option(spred: BiFunction<String, CharSequence, Boolean>, vararg option: String?): ComponentQuery {
        root = root.and { t ->
            val opts = t.options
            opts != null && option.any { o -> o != null && opts.any { j -> j != null && spred.apply(o, j) } }
        }
        return this
    }

    /** Convenience overload that keeps components exposing any of the literal option strings. */
    fun option(vararg option: String): ComponentQuery = option(BiFunction { a, b -> a.contentEquals(b) }, *option)

    /** Case-insensitive option equality helper. */
    fun optionEqualsIgnoreCase(vararg options: String): ComponentQuery = option(StringMatchers.equalsIgnoreCase, *options)

    /** Keeps components exposing options that contain any of the provided fragments. */
    fun optionContains(vararg fragments: String): ComponentQuery = option(StringMatchers.contains, *fragments)

    /** Case-insensitive containment helper for component options. */
    fun optionContainsIgnoreCase(vararg fragments: String): ComponentQuery = option(StringMatchers.containsIgnoreCase, *fragments)

    /** Matches components that define any of the supplied parameter keys. */
    fun params(vararg params: Int): ComponentQuery {
        root = root.and { t -> params.any { p -> t.params.containsKey(p) } }
        return this
    }

    /** Keeps parent components that have at least one child whose id is in [ids]. */
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

/** Fluent alias so chains read as `query withText "Deposit-all"`. */
infix fun ComponentQuery.withText(text: String): ComponentQuery = this.text(text)

/** Alias for [ComponentQuery.option] exposed as `query withOption "Select"`. */
infix fun ComponentQuery.withOption(option: String): ComponentQuery = this.option(option)

