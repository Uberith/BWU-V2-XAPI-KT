package net.botwithus.kxapi.game.skilling.impl.fletching

/**
 * Canonical grouping of RuneScape 3 fletching categories as exposed by the production interface (1371).
 *
 * Each enum entry mirrors a tab in the Make-X interface to simplify automation logic.
 * [interfaceName] is the label used by the menu widget, while [displayName] is a friendlier alias used in logs.
 *
 * The flag [membersOnly] defaults to true because Fletching is inherently a members skill. If Jagex ever
 * makes a category available on free worlds it can be overridden on the individual recipe.
 */
enum class FletchingCategory(
    val displayName: String,
    val interfaceName: String,
    val aliases: Set<String> = emptySet(),
    val membersOnly: Boolean = true
) {
    AMMO(
        displayName = "Ammo",
        interfaceName = "Ammo",
        aliases = setOf("Ammunition")
    ),
    SHORTBOWS(
        displayName = "Shortbows",
        interfaceName = "Shortbows"
    ),
    SHIELDBOWS(
        displayName = "Shieldbows",
        interfaceName = "Shieldbows",
        aliases = setOf("Longbows")
    ),
    CROSSBOWS(
        displayName = "Crossbows",
        interfaceName = "Crossbows"
    ),
    BOLTS(
        displayName = "Bolts",
        interfaceName = "Bolts"
    ),
    DARTS(
        displayName = "Darts",
        interfaceName = "Darts"
    ),
    PROTEAN(
        displayName = "Protean",
        interfaceName = "Protean"
    );

    fun matches(label: String): Boolean {
        val candidate = label.trim()
        return candidate.equals(interfaceName, ignoreCase = true) ||
            candidate.equals(displayName, ignoreCase = true) ||
            aliases.any { candidate.equals(it, ignoreCase = true) }
    }
}

/**
 * Canonical list of supported Fletching recipes handled by the helper API.
 *
 * Each entry captures the minimum information we routinely need when scripting:
 * - [displayName]/[itemName]: user facing labels vs interface selection name.
 * - [category]: the production interface tab in which the item resides.
 * - [levelReq]: minimum Fletching level required.
 * - [membersOnly]: whether the recipe is restricted beyond the inherent members requirement of the skill.
 * - [primaryMaterial]/[secondaryMaterial]: quick references for inventory validation.
 */
enum class FletchingProduct(
    val displayName: String,
    val itemName: String,
    val category: FletchingCategory,
    val levelReq: Int,
    val membersOnly: Boolean = category.membersOnly,
    val primaryMaterial: String? = null,
    val secondaryMaterial: String? = null
) {
    ARROW_SHAFTS(
        displayName = "Arrow shafts",
        itemName = "Arrow shafts",
        category = FletchingCategory.AMMO,
        levelReq = 1,
        primaryMaterial = "Logs"
    ),
    HEADLESS_ARROWS(
        displayName = "Headless arrows",
        itemName = "Headless arrows",
        category = FletchingCategory.AMMO,
        levelReq = 1,
        primaryMaterial = "Arrow shafts",
        secondaryMaterial = "Feathers"
    ),
    BRONZE_ARROWS(
        displayName = "Bronze arrows",
        itemName = "Bronze arrows",
        category = FletchingCategory.AMMO,
        levelReq = 1,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Bronze arrowheads"
    ),
    IRON_ARROWS(
        displayName = "Iron arrows",
        itemName = "Iron arrows",
        category = FletchingCategory.AMMO,
        levelReq = 15,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Iron arrowheads"
    ),
    STEEL_ARROWS(
        displayName = "Steel arrows",
        itemName = "Steel arrows",
        category = FletchingCategory.AMMO,
        levelReq = 30,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Steel arrowheads"
    ),
    MITHRIL_ARROWS(
        displayName = "Mithril arrows",
        itemName = "Mithril arrows",
        category = FletchingCategory.AMMO,
        levelReq = 45,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Mithril arrowheads"
    ),
    ADAMANT_ARROWS(
        displayName = "Adamant arrows",
        itemName = "Adamant arrows",
        category = FletchingCategory.AMMO,
        levelReq = 60,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Adamant arrowheads"
    ),
    RUNE_ARROWS(
        displayName = "Rune arrows",
        itemName = "Rune arrows",
        category = FletchingCategory.AMMO,
        levelReq = 75,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Rune arrowheads"
    ),
    DRAGON_ARROWS(
        displayName = "Dragon arrows",
        itemName = "Dragon arrows",
        category = FletchingCategory.AMMO,
        levelReq = 90,
        secondaryMaterial = "Dragon arrowheads",
        primaryMaterial = "Headless arrows"
    ),
    BROAD_ARROWS(
        displayName = "Broad arrows",
        itemName = "Broad arrows",
        category = FletchingCategory.AMMO,
        levelReq = 50,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Broad arrowheads"
    ),
    ELDER_ARROWS(
        displayName = "Elder arrows",
        itemName = "Elder arrows",
        category = FletchingCategory.AMMO,
        levelReq = 95,
        primaryMaterial = "Headless arrows",
        secondaryMaterial = "Elder arrowheads"
    ),
    SHORTBOW_UNSTRUNG(
        displayName = "Shortbow (u)",
        itemName = "Shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 5,
        primaryMaterial = "Logs"
    ),
    OAK_SHORTBOW_UNSTRUNG(
        displayName = "Oak shortbow (u)",
        itemName = "Oak shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 20,
        primaryMaterial = "Oak logs"
    ),
    WILLOW_SHORTBOW_UNSTRUNG(
        displayName = "Willow shortbow (u)",
        itemName = "Willow shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 35,
        primaryMaterial = "Willow logs"
    ),
    MAPLE_SHORTBOW_UNSTRUNG(
        displayName = "Maple shortbow (u)",
        itemName = "Maple shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 50,
        primaryMaterial = "Maple logs"
    ),
    YEW_SHORTBOW_UNSTRUNG(
        displayName = "Yew shortbow (u)",
        itemName = "Yew shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 65,
        primaryMaterial = "Yew logs"
    ),
    MAGIC_SHORTBOW_UNSTRUNG(
        displayName = "Magic shortbow (u)",
        itemName = "Magic shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 80,
        primaryMaterial = "Magic logs"
    ),
    ELDER_SHORTBOW_UNSTRUNG(
        displayName = "Elder shortbow (u)",
        itemName = "Elder shortbow (u)",
        category = FletchingCategory.SHORTBOWS,
        levelReq = 90,
        primaryMaterial = "Elder logs"
    ),
    SHIELDBOW_UNSTRUNG(
        displayName = "Shieldbow (u)",
        itemName = "Shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 10,
        primaryMaterial = "Logs"
    ),
    OAK_SHIELDBOW_UNSTRUNG(
        displayName = "Oak shieldbow (u)",
        itemName = "Oak shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 25,
        primaryMaterial = "Oak logs"
    ),
    WILLOW_SHIELDBOW_UNSTRUNG(
        displayName = "Willow shieldbow (u)",
        itemName = "Willow shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 40,
        primaryMaterial = "Willow logs"
    ),
    MAPLE_SHIELDBOW_UNSTRUNG(
        displayName = "Maple shieldbow (u)",
        itemName = "Maple shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 55,
        primaryMaterial = "Maple logs"
    ),
    YEW_SHIELDBOW_UNSTRUNG(
        displayName = "Yew shieldbow (u)",
        itemName = "Yew shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 70,
        primaryMaterial = "Yew logs"
    ),
    MAGIC_SHIELDBOW_UNSTRUNG(
        displayName = "Magic shieldbow (u)",
        itemName = "Magic shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 85,
        primaryMaterial = "Magic logs"
    ),
    ELDER_SHIELDBOW_UNSTRUNG(
        displayName = "Elder shieldbow (u)",
        itemName = "Elder shieldbow (u)",
        category = FletchingCategory.SHIELDBOWS,
        levelReq = 95,
        primaryMaterial = "Elder logs"
    );

    companion object {
        private val NAME_INDEX: Map<String, FletchingProduct> = entries.associateBy { it.itemName.lowercase() }
        private val DISPLAY_INDEX: Map<String, FletchingProduct> = entries.associateBy { it.displayName.lowercase() }

        fun byName(name: String): FletchingProduct? {
            val key = name.trim().lowercase()
            return NAME_INDEX[key] ?: DISPLAY_INDEX[key]
        }
    }
}
