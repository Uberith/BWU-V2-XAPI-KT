package net.botwithus.kxapi.game.skilling.impl.woodcutting

import java.util.regex.Pattern

/**
 * Canonical list of supported tree types for woodcutting.
 *
 * Each enum value encapsulates the common metadata we need when scripting:
 * - displayName: Human‑friendly name as used in UI/logging.
 * - logName: The resulting log name for inventory/bank interactions.
 * - namePattern: Case‑insensitive regex used to match scene object names.
 * - typeIds: Scene object type ids for the choppable variants (optional).
 * - excludedIds: Specific scene object ids to ignore (e.g., unchoppable variants).
 * - levelReq: Minimum Woodcutting level typically required.
 * - membersOnly: Whether the tree is restricted to members worlds.
 *
 * Note:
 * - The namePattern is intentionally strict for reliability (exact name match, case‑insensitive).
 * - Excluded ids are a practical safeguard to avoid interacting with visually similar but invalid trees.
 */
enum class TreeType(
    val displayName: String,
    val logName: String,
    val namePattern: Pattern,
    /** Known choppable scene object ids for this tree (optional) */
    val typeIds: Set<Int> = emptySet(),
    val excludedIds: Set<Int> = emptySet(),
    val levelReq: Int,
    val membersOnly: Boolean = false
)
{
    TREE(
        displayName = "Tree",
        logName = "Logs",
        namePattern = Pattern.compile("^Tree$", Pattern.CASE_INSENSITIVE),
        typeIds = setOf(38788),
        levelReq = 1,
        membersOnly = false
    ),
    OAK(
        displayName = "Oak",
        logName = "Oak logs",
        namePattern = Pattern.compile("^Oak$", Pattern.CASE_INSENSITIVE),
        typeIds = setOf(38371),
        levelReq = 10,
        membersOnly = false
    ),
    WILLOW(
        displayName = "Willow",
        logName = "Willow logs",
        namePattern = Pattern.compile("^Willow$", Pattern.CASE_INSENSITIVE),
        levelReq = 20,
        membersOnly = false
    ),
    MAPLE(
        displayName = "Maple Tree",
        logName = "Maple logs",
        namePattern = Pattern.compile("^Maple Tree$", Pattern.CASE_INSENSITIVE),
        levelReq = 40,
        membersOnly = false
    ),
    YEW(
        displayName = "Yew",
        logName = "Yew logs",
        namePattern = Pattern.compile("^Yew$", Pattern.CASE_INSENSITIVE),
        levelReq = 70,
        membersOnly = false
    ),
    MAGIC(
        displayName = "Magic tree",
        logName = "Magic logs",
        namePattern = Pattern.compile("^Magic tree$", Pattern.CASE_INSENSITIVE),
        excludedIds = setOf(129121),
        levelReq = 80,
        membersOnly = true
    ),
    ELDER(
        displayName = "Elder tree",
        logName = "Elder logs",
        namePattern = Pattern.compile("^Elder tree$", Pattern.CASE_INSENSITIVE),
        excludedIds = setOf(87509),
        levelReq = 90,
        membersOnly = true
    ),
    TEAK(
        displayName = "Teak",
        logName = "Teak logs",
        namePattern = Pattern.compile("^Teak$", Pattern.CASE_INSENSITIVE),
        levelReq = 30,
        membersOnly = true
    ),
    MAHOGANY(
        displayName = "Mahogany",
        logName = "Mahogany logs",
        namePattern = Pattern.compile("^Mahogany$", Pattern.CASE_INSENSITIVE),
        levelReq = 60,
        membersOnly = true
    ),
    ACADIA(
        displayName = "Acadia tree",
        logName = "Acadia logs",
        namePattern = Pattern.compile("^Acadia tree$", Pattern.CASE_INSENSITIVE),
        levelReq = 50,
        membersOnly = true
    ),
    ETERNAL_MAGIC(
        displayName = "Eternal magic tree",
        logName = "Magic logs",
        namePattern = Pattern.compile("^Eternal magic tree$", Pattern.CASE_INSENSITIVE),
        levelReq = 100,
        membersOnly = true
    ),
    IVY(
        displayName = "Ivy",
        logName = "Ivy",
        namePattern = Pattern.compile("^Ivy$", Pattern.CASE_INSENSITIVE),
        levelReq = 68,
        membersOnly = false
    ),
    ARCTIC_PINE(
        displayName = "Arctic Pine",
        logName = "Arctic pine logs",
        namePattern = Pattern.compile("^Arctic Pine$", Pattern.CASE_INSENSITIVE),
        levelReq = 54,
        membersOnly = true
    ),
    EUCALYPTUS(
        displayName = "Eucalyptus tree",
        logName = "Eucalyptus logs",
        namePattern = Pattern.compile("^Eucalyptus tree$", Pattern.CASE_INSENSITIVE),
        levelReq = 58,
        membersOnly = true
    ),
    CRYSTAL(
        displayName = "Crystal tree",
        logName = "Crystal tree blossom",
        namePattern = Pattern.compile("^Crystal tree$", Pattern.CASE_INSENSITIVE),
        levelReq = 94,
        membersOnly = true
    ),
    BAMBOO(
        displayName = "Bamboo",
        logName = "Bamboo",
        namePattern = Pattern.compile("^Bamboo$", Pattern.CASE_INSENSITIVE),
        levelReq = 90,
        membersOnly = true
    );
}
