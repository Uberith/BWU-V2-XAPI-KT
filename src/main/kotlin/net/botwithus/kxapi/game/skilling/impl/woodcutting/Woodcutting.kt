@file:Suppress("UNUSED_PARAMETER")

package net.botwithus.kxapi.game.skilling.impl.woodcutting

import net.botwithus.kxapi.game.skilling.Skilling
import net.botwithus.kxapi.game.skilling.skilling
import net.botwithus.kxapi.script.SuspendableScript
import net.botwithus.rs3.entities.SceneObject
import net.botwithus.xapi.query.SceneObjectQuery
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Woodcutting-only helpers that locate trees and issue the "Chop" interaction.
 *
 * - Stateless: every invocation performs a fresh query against the live scene.
 * - No movement, banking, boosts or hopping logic is included here.
 * - Ergonomic overloads are provided for [TreeType], display names, and regular expressions.
 */
class Woodcutting internal constructor(val skilling: Skilling) {

    fun chop(): TreeChopRequestBuilder = TreeChopRequestBuilder()

    fun chop(type: TreeType): TreeChopRequest = chop().type(type).build()

    fun chop(name: String): TreeChopRequest = chop().name(name.trim()).build()

    fun chop(target: SceneObject): Boolean {
        logger.debug("chop(target): name='{}', typeId={}", target.name, target.typeId)
        return interact(target)
    }

    fun nearest(type: TreeType): SceneObject? {
        logger.debug("nearest(type='{}'): begin", type.displayName)
        val obj = treeQuery().name(type.namePattern).option(*PREFERRED_ACTIONS_ARRAY)
            .results().nearest() ?: run {
            logger.debug("nearest(type='{}'): none found", type.displayName)
            return null
        }

        if (type.excludedIds.contains(obj.typeId)) {
            logger.debug("nearest(type='{}'): excluded typeId {}", type.displayName, obj.typeId)
            return null
        }
        logger.debug(" nearest(type='{}'): name='{}', typeId={}, options={}", type.displayName, obj.name, obj.typeId, obj.options)
        return obj
    }

    fun nearest(): SceneObject? {
        logger.debug("nearest(): begin (any tree)")
        val obj = treeQuery().option(*PREFERRED_ACTIONS_ARRAY).results().nearest() ?: run {
            logger.debug("nearest(): none found")
            return null
        }
        logger.debug("nearest(): name='{}', typeId={}, options={}", obj.name, obj.typeId, obj.options)
        return obj
    }

    fun nearest(pattern: Pattern): SceneObject? {
        logger.debug("nearest(pattern='{}'): begin", pattern.pattern())
        val obj = treeQuery().name(pattern).results().firstOrNull() ?: run {
            logger.debug("nearest(pattern='{}'): none found", pattern.pattern())
            return null
        }
        logger.debug("nearest(pattern='{}'): name='{}', typeId={}, options={}", pattern.pattern(), obj.name, obj.typeId, obj.options)
        return obj
    }

    fun nearest(name: String): SceneObject? {
        val norm = toTitleCase(name.trim())
        logger.debug("nearest(name): raw='{}', normalized='{}'", name, norm)

        resolveTreeType(norm)?.let { type ->
            logger.debug("nearest(name): resolved to type='{}'", type.displayName)
            return nearest(type)
        }

        val contains: (String, CharSequence) -> Boolean = { needle, hay ->
            hay.toString().contains(needle, ignoreCase = true)
        }
        val obj = treeQuery().name(contains, norm).option(contains, "chop")
            .results().firstOrNull() ?: run {
            logger.debug("nearest(name): no fallback match for '{}'", name)
            return null
        }
        logger.debug("nearest(name): fallback name='{}', typeId={}", obj.name, obj.typeId)
        return obj
    }

    fun count(type: TreeType): Int {
        val size = SceneObjectQuery.newQuery().name(type.namePattern).hidden(false).results().size()
        logger.debug("count('{}'): {}", type.displayName, size)
        return size
    }

    suspend fun chopAndAwait(type: TreeType) = requireSuspendableScript().awaitPostChop(chop(type).nearest())

    suspend fun chopAndAwait(target: SceneObject) = requireSuspendableScript().awaitPostChop(chop(target))

    suspend fun chopAndAwait(name: String) = requireSuspendableScript().awaitPostChop(chop(name).nearest())

    fun resolveTreeType(name: String): TreeType? {
        val norm = toTitleCase(name.trim())
        return TREE_TYPES.firstOrNull { type ->
            val display = type.displayName
            val short = display.removeSuffix(" tree").removeSuffix(" Tree")
            norm.equals(display, ignoreCase = true) || norm.equals(short, ignoreCase = true)
        }
    }

    fun all(): List<TreeType> = TREE_TYPES.toList()

    fun allNamesCamelCase(): List<String> = TREE_TYPES.map { toTitleCase(it.displayName) }

    fun allWithCamelCaseNames(): List<Pair<TreeType, String>> = TREE_TYPES.map { it to toTitleCase(it.displayName) }

    fun availableFor(level: Int, isMember: Boolean): List<TreeType> =
        TREE_TYPES.filter { level >= it.levelReq && (!it.membersOnly || isMember) }

    fun bestFor(level: Int, isMember: Boolean): TreeType? = availableFor(level, isMember).maxByOrNull { it.levelReq }

    class TreeChopRequest internal constructor(
        private val locator: () -> SceneObject?,
        private val interactor: (SceneObject) -> Boolean
    ) {
        fun nearest(): Boolean = locator()?.let(interactor) ?: false

        fun nearestObject(): SceneObject? = locator()

        fun target(target: SceneObject?): Boolean = target?.let(interactor) ?: false
    }

    private sealed interface Lookup {
        object Any : Lookup
        data class Type(val type: TreeType) : Lookup
        data class Exact(val pattern: Pattern) : Lookup
    }

    inner class TreeChopRequestBuilder internal constructor() {

        private var lookup: Lookup = Lookup.Any

        fun any(): TreeChopRequestBuilder = apply { lookup = Lookup.Any }

        fun type(type: TreeType): TreeChopRequestBuilder = apply { lookup = Lookup.Type(type) }

        fun name(name: String): TreeChopRequestBuilder = apply {
            val trimmed = name.trim()
            lookup = resolveTreeType(trimmed)?.let(Lookup::Type)
                ?: Lookup.Exact(exactNamePattern(trimmed))
        }

        fun pattern(pattern: Pattern): TreeChopRequestBuilder = apply { lookup = Lookup.Exact(pattern) }

        fun nearest(): Boolean = locate()?.let(this@Woodcutting::chop) ?: false

        fun nearestObject(): SceneObject? = locate()

        fun target(target: SceneObject?): Boolean = target?.let(this@Woodcutting::chop) ?: false

        fun build(): TreeChopRequest = TreeChopRequest(locator = ::locate, interactor = this@Woodcutting::chop)

        private fun locate(): SceneObject? = when (val spec = lookup) {
            Lookup.Any -> this@Woodcutting.nearest()
            is Lookup.Type -> this@Woodcutting.nearest(spec.type)
            is Lookup.Exact -> this@Woodcutting.nearest(spec.pattern)
        }

        private fun exactNamePattern(name: String): Pattern =
            Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE)
    }

    private fun treeQuery() = SceneObjectQuery.newQuery().hidden(false)

    private fun chooseAction(options: List<String?>): String =
        PREFERRED_ACTIONS.firstOrNull { pref -> options.any { it?.equals(pref, ignoreCase = true) == true } }
            ?: PREFERRED_ACTIONS.first()

    private fun interact(obj: SceneObject): Boolean {
        val options = obj.options ?: emptyList()
        val action = chooseAction(options)
        logger.debug("interact: name='{}', typeId={}, action='{}', options={}", obj.name, obj.typeId, action, options)
        val ok = obj.interact(action) > 0
        if (!ok) {
            logger.warn("interact failed: name='{}', typeId={}, action='{}'", obj.name, obj.typeId, action)
        }
        return ok
    }

    private fun requireSuspendableScript(): SuspendableScript {
        val script = skilling.script
        return script as? SuspendableScript
            ?: error("Skilling.woodcutting requires a SuspendableScript (was ${script::class.simpleName})")
    }

    private suspend fun SuspendableScript.awaitPostChop(success: Boolean): Boolean {
        if (success) awaitTicks(2)
        return success
    }

    private fun toTitleCase(text: String): String =
        text.trim().lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { ch -> ch.titlecase() } }

    private companion object {
        private val logger = LoggerFactory.getLogger(Woodcutting::class.java)
        private val PREFERRED_ACTIONS = listOf("Chop down", "Cut down", "Chop")
        private val PREFERRED_ACTIONS_ARRAY = PREFERRED_ACTIONS.toTypedArray()
        private val TREE_TYPES = TreeType.entries.toTypedArray()
    }
}

val Skilling.woodcutting: Woodcutting
    get() = Woodcutting(this)

suspend fun Skilling.chop(type: TreeType): Boolean = woodcutting.chopAndAwait(type)

suspend fun Skilling.chop(target: SceneObject): Boolean = woodcutting.chopAndAwait(target)

suspend fun Skilling.chop(name: String): Boolean = woodcutting.chopAndAwait(name)

suspend fun SuspendableScript.chop(type: TreeType): Boolean = this.skilling.chop(type)

suspend fun SuspendableScript.chop(target: SceneObject): Boolean = this.skilling.chop(target)

suspend fun SuspendableScript.chop(name: String): Boolean = this.skilling.chop(name)

