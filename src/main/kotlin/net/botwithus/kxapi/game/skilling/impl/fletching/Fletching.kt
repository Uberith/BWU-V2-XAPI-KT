package net.botwithus.kxapi.game.skilling.impl.fletching

import net.botwithus.kxapi.game.skilling.Skilling
import net.botwithus.kxapi.game.skilling.production.ProductionResult
import net.botwithus.kxapi.game.skilling.production.ProductionManager
import net.botwithus.kxapi.game.skilling.production.ProductionMessage
import net.botwithus.kxapi.game.skilling.skilling
import net.botwithus.kxapi.script.SuspendableScript
import org.slf4j.LoggerFactory

/**
 * Convenience helpers around the RuneScape 3 Fletching production interface.
 *
 * The API mirrors the ergonomics of the woodcutting helpers: it exposes strongly typed recipes, query utilities
 * and wrappers that hand back a [ProductionManager] ready to interact with interface 1371.
 */
class Fletching internal constructor(private val skilling: Skilling) {

    /**
     * Creates a production manager configured for the supplied [product] but does not start the interaction.
     * The caller can further customise the manager before invoking [ProductionManager.produceItem].
     */
    fun produce(product: FletchingProduct): ProductionManager {
        logger.debug(
            "produce(product='{}', category='{}')",
            product.displayName,
            product.category.interfaceName
        )
        return skilling.production {
            itemName(product.itemName)
            category(product.category.interfaceName)
        }
    }

    /**
     * Convenience wrapper that immediately calls [ProductionManager.produceItem] for the given [product].
     */
    fun produce(
        product: FletchingProduct,
        onFinished: (Double) -> Unit = { _ -> },
        onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
    ) {
        produce(product).produceItem(onFinished, onProgress)
    }

    /**
     * Creates a production manager using a raw [category] and [itemName], letting scripts handle items that are
     * not yet represented as a [FletchingProduct].
     */
    fun produce(category: FletchingCategory, itemName: String): ProductionManager {
        val trimmedItem = itemName.trim()
        logger.debug(
            "produce(category='{}', item='{}')",
            category.interfaceName,
            trimmedItem
        )
        return skilling.production {
            itemName(trimmedItem)
            category(category.interfaceName)
        }
    }

    /**
     * Low-level escape hatch when only the raw interface label is known.
     */
    fun produce(categoryName: String, itemName: String): ProductionManager {
        val trimmedCategory = categoryName.trim()
        val trimmedItem = itemName.trim()
        logger.debug("produce(categoryName='{}', item='{}')", trimmedCategory, trimmedItem)
        return skilling.produce(trimmedItem, trimmedCategory)
    }

    /**
     * Attempts to resolve [name] against the canonical recipe list and returns a ready manager if successful.
     */
    fun produce(name: String): ProductionManager? = resolveProduct(name)?.let(::produce)

    /**
     * Resolves [name] and, if successful, immediately starts production with the provided callbacks.
     */
    fun produce(
        name: String,
        onFinished: (Double) -> Unit = { _ -> },
        onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
    ) {
        val product = resolveProduct(name)
            ?: error("Unknown fletching product '$name'")
        produce(product, onFinished, onProgress)
    }

    /**
     * Quick visibility into whether the current interface state has the required resources for [product].
     * Returns false when interface 1371 is closed or when the player lacks materials.
     */
    fun canProduce(product: FletchingProduct): Boolean = produce(product).canProduce()

    /**
     * Resolves a product by display or item name.
     */
    fun resolveProduct(name: String): FletchingProduct? {
        val match = FletchingProduct.byName(name)
        if (match == null) {
            logger.debug("resolveProduct('{}'): no canonical match", name)
        }
        return match
    }

    /**
     * All supported recipes, optionally filtered to a specific [category].
     */
    fun all(category: FletchingCategory? = null): List<FletchingProduct> =
        if (category == null) PRODUCTS else PRODUCTS.filter { it.category == category }

    /**
     * Returns every category helper currently modelled.
     */
    fun categories(): List<FletchingCategory> = FletchingCategory.entries.toList()

    /**
     * Attempts to match a raw interface label to one of the known categories.
     */
    fun resolveCategory(label: String): FletchingCategory? =
        FletchingCategory.entries.firstOrNull { it.matches(label) }

    /**
     * Filters recipes that are unlocked for the provided account state.
     */
    fun availableFor(
        level: Int,
        isMember: Boolean,
        category: FletchingCategory? = null
    ): List<FletchingProduct> = PRODUCTS.filter { product ->
        level >= product.levelReq &&
            (isMember || !product.membersOnly) &&
            (category == null || product.category == category)
    }

    /**
     * Returns the highest-level recipe currently available for the given criteria.
     */
    fun bestFor(
        level: Int,
        isMember: Boolean,
        category: FletchingCategory? = null
    ): FletchingProduct? = availableFor(level, isMember, category).maxByOrNull { it.levelReq }

    /**
     * Provides a quick material summary for inventory validation logic.
     */
    fun materialsOf(product: FletchingProduct): Pair<String?, String?> =
        product.primaryMaterial to product.secondaryMaterial

    companion object {
        private val logger = LoggerFactory.getLogger(Fletching::class.java)
        private val PRODUCTS = FletchingProduct.entries.toList()
    }
}

/** Lazily initialised fletching helpers for the current script. */
val Skilling.fletching: Fletching
    get() = Fletching(this)

/** Convenience shortcut for starting a canonical recipe directly from [Skilling]. */
fun Skilling.fletch(
    product: FletchingProduct,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = fletching.produce(product, onFinished, onProgress)

/** Starts an ad-hoc recipe resolved by name. */
fun Skilling.fletch(
    name: String,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = fletching.produce(name, onFinished, onProgress)

/** Starts a recipe by explicitly specifying the interface category. */
fun Skilling.fletch(
    category: FletchingCategory,
    itemName: String,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = fletching.produce(category, itemName).produceItem(onFinished, onProgress)

/**
 * SuspendableScript-friendly overloads that retain the suspension semantics of other skilling helpers.
 */
fun SuspendableScript.fletch(
    product: FletchingProduct,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = this.skilling.fletch(product, onFinished, onProgress)

fun SuspendableScript.fletch(
    name: String,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = this.skilling.fletch(name, onFinished, onProgress)

fun SuspendableScript.fletch(
    category: FletchingCategory,
    itemName: String,
    onFinished: (Double) -> Unit = { _ -> },
    onProgress: (ProductionMessage<ProductionResult>, Int, Int, Int, Double) -> Unit = { _, _, _, _, _ -> }
) = this.skilling.fletching.produce(category, itemName).produceItem(onFinished, onProgress)
