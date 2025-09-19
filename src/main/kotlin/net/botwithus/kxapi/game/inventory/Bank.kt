package net.botwithus.kxapi.game.inventory

import net.botwithus.kxapi.game.inventory.internal.TransferOptionType
import net.botwithus.kxapi.game.query.ComponentQuery
import net.botwithus.kxapi.game.query.InventoryItemQuery
import net.botwithus.kxapi.game.query.NpcQuery
import net.botwithus.kxapi.game.query.SceneObjectQuery
import net.botwithus.kxapi.game.query.result.ResultSet
import net.botwithus.kxapi.game.query.result.nearest
import net.botwithus.kxapi.script.SuspendableScript
import kotlin.text.lowercase

import net.botwithus.rs3.interfaces.Component
import net.botwithus.rs3.inventories.Inventory
import net.botwithus.rs3.inventories.InventoryManager
import net.botwithus.rs3.item.InventoryItem
import net.botwithus.rs3.item.Item
import net.botwithus.rs3.minimenu.Action
import net.botwithus.rs3.minimenu.MiniMenu
import net.botwithus.rs3.vars.VarDomain
import net.botwithus.rs3.world.Distance
import net.botwithus.util.Rand
import net.botwithus.xapi.script.permissive.base.PermissiveScript
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.BiFunction
import java.util.regex.Pattern
import java.util.stream.Collectors

object Bank {
    /**
     * Varbit/Varp ids and other static configuration used by the Bank UI.
     *
     * PRESET_BROWSING_VARBIT_ID toggles the preset-browsing page (10+ presets). The loadPreset flow
     * will click the appropriate tab first when necessary so that the requested preset index is
     * visible.
     */
    private const val PRESET_BROWSING_VARBIT_ID = 49662
    private const val SELECTED_OPTIONS_TAB_VARBIT_ID = 45191
    private const val WITHDRAW_TYPE_VARBIT_ID = 45189
    private const val WITHDRAW_X_VARP_ID = 111

    /**
     * Regex used to identify bank-related scene objects in the world. This excludes objects whose
     * name contains "deposit" to avoid matching deposit-only decorations.
     */
    private val BANK_NAME_PATTERN: Pattern = Pattern.compile("^(?!.*deposit).*(bank|counter).*$", Pattern.CASE_INSENSITIVE)
    private const val LAST_PRESET_OPTION = "Load Last Preset from"
    private const val INVENTORY_ID = 95
    private const val INTERFACE_INDEX = 517
    private const val COMPONENT_INDEX = 202

    private val logger: Logger = LoggerFactory.getLogger(Bank::class.java)

    /**
     * Cache of the last successfully loaded preset index. Helpful for scripts that want to avoid
     * repeatedly loading the same preset in quick succession.
     */
    private var previousLoadedPreset = -1

    /**
     * Opens the bank by interacting with the nearest valid object or NPC.
     *
     * @param script logging + delay helper
     * @return true if an interaction was issued (not a guarantee the UI opened)
     */
    fun open(script: PermissiveScript): Boolean {
        return try {
            logger.info("Attempting find bank obj")
            val objUse = SceneObjectQuery.newQuery().name(BANK_NAME_PATTERN).option("Use").results().nearest()
            val objBank = SceneObjectQuery.newQuery().name(BANK_NAME_PATTERN).option("Bank").results().nearest()
            val chest = SceneObjectQuery.newQuery().name("Shantay chest").results().nearest()
            val obj = listOfNotNull(objUse, objBank, chest).minByOrNull { Distance.to(it) }

            logger.info("Attempting find bank npc")
            val npc = NpcQuery.newQuery().option("Bank").results().nearest()
            logger.info("Bank opening initiated")
            var useObj = true

            logger.info("Object is ${if (obj != null) "not null" else "null"}")
            logger.info("Npc is ${if (npc != null) "not null" else "null"}")

            if (obj != null && npc != null) {
                logger.info("Distance.to(obj): ${Distance.to(obj)}")
                logger.info("Distance.to(npc): ${Distance.to(npc)}")
                val objDist = Distance.to(obj)
                val npcDist = Distance.to(npc)
                if (!objDist.isNaN() && !npcDist.isNaN()) {
                    useObj = objDist < npcDist
                }
                logger.info("useObj: $useObj")
            }

            if (obj != null && useObj) {
                script.info("Interacting via Object: ${obj.name}")
                val actions = obj.options
                logger.info("Available Options: $actions")
                if (!actions.isEmpty()) {
                    val action = actions.stream().filter { it != null && it.isNotEmpty() }.findFirst()
                    logger.info("action.isPresent(): ${action.isPresent}")
                    if (action.isPresent) {
                        logger.info("Attempting to interact with bank object using action: ${action.get()}")
                        val interactionResult = obj.interact(action.get())
                        script.info("Object interaction completed: $interactionResult")
                        interactionResult > 0
                    } else {
                        script.warn("No valid action found for bank object")
                        false
                    }
                } else {
                    script.warn("No options available on bank object")
                    false
                }
            } else if (npc != null) {
                script.info("Interacting via NPC")
                val interactionResult = npc.interact("Bank")
                script.info("NPC interaction completed: $interactionResult")
                interactionResult > 0
            } else {
                script.warn("No valid bank object or NPC found")
                false
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            false
        }
    }

    /**
     * Checks whether the bank interface is open.
     *
     * @return true if the bank UI is currently visible
     */
    fun isOpen(): Boolean {
        // Try direct platform API via reflection to avoid hard module dependency issues
        try {
            val cls = Class.forName("net.botwithus.rs3.interfaces.Interfaces")
            val m = cls.getMethod("isOpen", Int::class.javaPrimitiveType)
            val open = (m.invoke(null, INTERFACE_INDEX) as? Boolean) ?: false
            logger.debug("[Bank] isOpen via Interfaces.isOpen({}) -> {}", INTERFACE_INDEX, open)
            return open
        } catch (t: Throwable) {
            logger.debug("[Bank] Interfaces.isOpen reflection failed: {}", t.message)
        }
        // Fallback: presence of a known bank component under interface 517
        return try {
            val comp = ComponentQuery.newQuery(INTERFACE_INDEX).id(COMPONENT_INDEX).results().first()
            val open = comp != null
            logger.debug("[Bank] isOpen via component probe (iface={}, compId={}) -> {}", INTERFACE_INDEX, COMPONENT_INDEX, open)
            open
        } catch (t: Throwable) {
            logger.debug("[Bank] isOpen component probe failed: {}", t.message)
            false
        }
    }
    /**
     * Closes the bank via the close-button component action.
     *
     * @return true if a click action was issued
     */
    fun close(): Boolean = MiniMenu.doAction(Action.COMPONENT, 1, -1, 33882430) > 0

    /**
     * Gets the bank inventory handle (id = 95).
     *
     * @return the bank [Inventory] or null if unavailable
     */
    fun getInventory(): Inventory? = InventoryManager.getInventory(INVENTORY_ID)

    /**
     * Loads the "Last preset" using the nearest valid object or NPC.
     *
     * @return true if an interaction to load the last preset was issued
     */
    fun loadLastPreset(): Boolean {
        val obj = SceneObjectQuery.newQuery().option(LAST_PRESET_OPTION).results().nearest()
        val npc = NpcQuery.newQuery().option(LAST_PRESET_OPTION).results().nearest()
        var useObj = true

        if (obj != null && npc != null) {
            val objDist = Distance.to(obj)
            val npcDist = Distance.to(npc)
            if (!objDist.isNaN() && !npcDist.isNaN())
                useObj = objDist < npcDist
        }
        return if (obj != null && useObj) {
            obj.interact(LAST_PRESET_OPTION) > 0
        } else if (npc != null) {
            npc.interact(LAST_PRESET_OPTION) > 0
        } else false
    }

    /** Returns a snapshot of non-empty item stacks currently visible in the bank container. */
    fun getItems(): Array<Item> =
        InventoryItemQuery.newQuery(INVENTORY_ID).results()
            .stream().filter { it.id != -1 }
            .map { it as Item }
            .toList()
            .toTypedArray()

    /**
     * Sums quantities across a result set of inventory items.
     *
     * @param results inventory items to total
     * @return total quantity across all entries
     */
    fun count(results: ResultSet<InventoryItem>): Int = results.stream().mapToInt(Item::getQuantity).sum()

    /**
     * Gets the first matching bank item for a query.
     *
     * @param query item query over bank inventory id (95)
     * @return the first matching [Item] or null
     */
    fun first(query: InventoryItemQuery): Item? = query.results().first()

    /**
     * Checks whether the bank has no items.
     *
     * @return true if the bank snapshot is empty
     */
    fun isEmpty(): Boolean = getItems().isEmpty()

    /**
     * Interacts with a bank item at [slot] using a UI option index.
     *
     * @param slot bank item slot (0-based)
     * @param option component option index (implementation-defined)
     * @return true if the interaction was issued
     */
    fun interact(slot: Int, option: Int): Boolean {
        val results: ResultSet<InventoryItem> = InventoryItemQuery.newQuery(INVENTORY_ID).slot(slot).results()
        val item = results.first()
        if (item == null) {
            logger.info("[Bank] interact(slot={}, option={}): no item in slot", slot, option)
            return false
        }
        logger.info("[Bank] interact(slot={}, option={}): item id={}, name='{}'", slot, option, item.id, item.name)

        // First try: grid component id + item id
        var comps = ComponentQuery.newQuery(INTERFACE_INDEX).id(COMPONENT_INDEX).itemId(item.id).results()
        logger.info("[Bank] interact(slot={}, option={}): grid match count={} (compId={})", slot, option, comps.size(), COMPONENT_INDEX)

        // Fallback: any component under interface with matching item id
        if (comps.size() == 0) {
            comps = ComponentQuery.newQuery(INTERFACE_INDEX).itemId(item.id).results()
            logger.info("[Bank] interact(slot={}, option={}): iface-wide match count={} (no compId filter)", slot, option, comps.size())
        }

        var comp = comps.first()
        if (comp == null) {
            // Last-ditch: scan all components (and children) for a withdraw-capable component
            // that references the item by name in text/optionBase.
            val all = ComponentQuery.newQuery(INTERFACE_INDEX).results().toList()
            fun hasWithdraw(c: net.botwithus.rs3.interfaces.Component): Boolean =
                try { c.options?.any { it != null && it.contains("Withdraw", ignoreCase = true) } == true } catch (_: Throwable) { false }
            fun nameMatch(c: net.botwithus.rs3.interfaces.Component): Boolean {
                val nm = item.name ?: return false
                val inText = try { (c.text ?: "").contains(nm, ignoreCase = true) } catch (_: Throwable) { false }
                val inBase = try { (c.optionBase ?: "").contains(nm, ignoreCase = true) } catch (_: Throwable) { false }
                return inText || inBase
            }
            comp = all.firstOrNull { hasWithdraw(it) && (it.itemId == item.id || nameMatch(it)) }
            if (comp == null) {
                // search children too
                val child = all.asSequence()
                    .mapNotNull { it.children }
                    .flatten()
                    .firstOrNull { hasWithdraw(it) && (it.itemId == item.id || nameMatch(it)) }
                comp = child
            }
            if (comp == null) {
                logger.info("[Bank] interact(slot={}, option={}): no component found for item id={} (name match failed)", slot, option, item.id)
                return false
            } else {
                logger.info("[Bank] interact(slot={}, option={}): using name-based fallback componentId={}", slot, option, comp.componentId)
            }
        }

        val opts = try { comp.options } catch (_: Throwable) { null }
        logger.info("[Bank] interact(slot={}, option={}): compId={}, options={} ", slot, option, comp.componentId, opts)

        // Prefer textual withdraw actions if present; fall back to numeric option index
        val ok = when {
            opts != null && option == 1 && opts.contains("Withdraw-1") -> {
                logger.info("[Bank] interact: using action 'Withdraw-1'")
                comp.interact("Withdraw-1") > 0
            }
            opts != null && opts.contains("Withdraw-All") -> {
                logger.info("[Bank] interact: using action 'Withdraw-All'")
                comp.interact("Withdraw-All") > 0
            }
            opts != null && opts.any { it != null && it.contains("Withdraw", ignoreCase = true) } -> {
                val first = opts.firstOrNull { it != null && it.contains("Withdraw", ignoreCase = true) }
                logger.info("[Bank] interact: using first withdraw action '{}'", first)
                first != null && comp.interact(first) > 0
            }
            else -> {
                logger.info("[Bank] interact: using numeric option {}", option)
                comp.interact(option) > 0
            }
        }
        logger.info("[Bank] interact(slot={}, option={}): result -> {}", slot, option, ok)
        return ok
    }

    /** Returns true if the given item query matches any items in the bank. */
    fun contains(query: InventoryItemQuery): Boolean = count(query.results()) > 0

    /** True if any of the provided item names are present in the bank. */
    fun contains(vararg itemNames: String): Boolean =
        !InventoryItemQuery.newQuery(INVENTORY_ID).name(*itemNames).results().isEmpty()

    /** True if any item with a name matching the regex pattern is present. */
    fun contains(itemNamePattern: Pattern): Boolean =
        !InventoryItemQuery.newQuery(INVENTORY_ID).name(itemNamePattern).results().isEmpty()

    /** Returns the total quantity across all stacks that match any of the provided names. */
    fun getCount(vararg itemNames: String): Int =
        count(InventoryItemQuery.newQuery(INVENTORY_ID).name(*itemNames).results())

    /** Returns the total quantity across all stacks whose name matches the given pattern. */
    fun getCount(namePattern: Pattern): Int =
        count(InventoryItemQuery.newQuery(INVENTORY_ID).name(namePattern).results())

    /**
     * Withdraws the first item matching [query] using the given option index.
     *
     * @param query item query over bank inventory id (95)
     * @param option option index to invoke
     * @return true if an interaction was issued
     */
    fun withdraw(query: InventoryItemQuery, option: Int): Boolean {
        logger.info("[Bank] withdraw(query, option={}): begin", option)
        setTransferOption(TransferOptionType.ALL)
        val item = query.results().first()
        if (item != null) {
            logger.info("[Bank] withdraw(query): item name='{}', id={}, slot={}", item.name, item.id, item.slot)
        } else {
            logger.info("[Bank] withdraw(query): no item matched")
        }
        val ok = item != null && interact(item.slot, option)
        logger.info("[Bank] withdraw(query, option={}): end -> {}", option, ok)
        return ok
    }

    /** Withdraws the first item matching the given name with the specified option index. */
    /**
     * Withdraws by exact item [itemName].
     * @param itemName exact item name (case-sensitive)
     * @param option option index to use
     * @return true if an interaction was sent
     */
    fun withdraw(itemName: String?, option: Int): Boolean {
        logger.info("[Bank] withdraw(name='{}', option={}): begin", itemName, option)
        val ok = if (!itemName.isNullOrEmpty())
            withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).name(itemName), option)
        else false
        logger.info("[Bank] withdraw(name='{}', option={}): end -> {}", itemName, option, ok)
        return ok
    }

    /** Withdraws by item id (if non-negative) using the specified option index. */
    /**
     * Withdraws by item id (if non-negative).
     * @param itemId item definition id
     * @param option option index
     * @return true if interaction was sent
     */
    fun withdraw(itemId: Int, option: Int): Boolean {
        logger.info("[Bank] withdraw(id={}, option={}): begin", itemId, option)
        val ok = if (itemId >= 0) withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).id(itemId), option) else false
        logger.info("[Bank] withdraw(id={}, option={}): end -> {}", itemId, option, ok)
        return ok
    }

    /** Withdraws the first item whose name matches the regex [pattern]. */
    /**
     * Withdraws the first item whose name matches [pattern].
     * @param pattern regex to match item names
     * @param option option index
     * @return true if interaction was sent
     */
    fun withdraw(pattern: Pattern?, option: Int): Boolean {
        logger.info("[Bank] withdraw(pattern={}, option={}): begin", pattern, option)
        val ok = if (pattern != null) withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).name(pattern), option) else false
        logger.info("[Bank] withdraw(pattern={}, option={}): end -> {}", pattern, option, ok)
        return ok
    }

    /**
     * Withdraws all of an item by exact name using option index 1.
     *
     * @param name exact item name
     * @return true if an interaction was issued
     */
    fun withdrawAll(name: String): Boolean = withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).name(name), 1)

    /**
     * Withdraws all of an item by id using option index 1.
     *
     * @param id item definition id
     * @return true if an interaction was issued
     */
    fun withdrawAll(id: Int): Boolean = withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).id(id), 1)

    /**
     * Withdraws all of an item matching [pattern] using option index 1.
     *
     * @param pattern regex to match item names
     * @return true if an interaction was issued
     */
    fun withdrawAll(pattern: Pattern): Boolean = withdraw(InventoryItemQuery.newQuery(INVENTORY_ID).name(pattern), 1)

    /**
     * Deposits carried items via the bank UI button.
     *
     * @return true if the “Deposit carried items” action was issued successfully
     */
    fun depositAll(): Boolean {
        logger.info("[Bank] depositAll(): begin")
        setTransferOption(TransferOptionType.ALL)
        val comp = ComponentQuery.newQuery(INTERFACE_INDEX).option("Deposit carried items").results().first()
        val found = comp != null
        logger.info("[Bank] depositAll(): component {}", if (found) "found" else "not-found")
        val ok = found && comp!!.interact(1) > 0
        logger.info("[Bank] depositAll(): interact -> {}", ok)
        return ok
    }

    /** Deposits equipment via its dedicated component in the bank UI. */
    /**
     * Deposits worn equipment using the bank UI control.
     * @return true if the click was sent
     */
    fun depositEquipment(): Boolean {
        val component = ComponentQuery.newQuery(INTERFACE_INDEX).id(42).results().first()
        return component != null && component.interact(1) > 0
    }

    /** Deposits the backpack via its dedicated component in the bank UI. */
    /**
     * Deposits backpack contents using the bank UI control.
     * @return true if the click was sent
     */
    fun depositBackpack(): Boolean {
        val component = ComponentQuery.newQuery(INTERFACE_INDEX).id(39).results().first()
        return component != null && component.interact(1) > 0
    }

    /** Deposits a component resolved by the provided query using a given option index. */
    /**
     * Deposits using a [ComponentQuery] to resolve the target, applying a specific option.
     *
     * @param script logging + delay helper
     * @param query component query targeting bank item components
     * @param option option index to invoke
     * @return true if interaction was sent
     */
    fun deposit(script: PermissiveScript, query: ComponentQuery, option: Int): Boolean {
        logger.info("[Bank] deposit(query, option={}): begin (isOpen={})", option, isOpen())
        val rs = query.results()
        logger.info("[Bank] deposit(query): result size={}", rs.size())
        if (rs.size() == 0) {
            val probe = ComponentQuery.newQuery(INTERFACE_INDEX).results()
            logger.info(
                "[Bank] deposit(query): iface {} total comps={} (sample up to 5 ids={})",
                INTERFACE_INDEX, probe.size(), probe.stream().limit(5).map { it.componentId }.toList()
            )
        }
        var item = rs.first()
        // If the resolved component does not expose Deposit actions, retarget to one that does
        if (item != null) {
            val opts = try { item.options } catch (_: Throwable) { null }
            val hasDeposit = opts != null && opts.any { it?.contains("Deposit", true) == true }
            val hasWithdraw = opts != null && opts.any { it?.contains("Withdraw", true) == true }
            if (!hasDeposit || hasWithdraw) {
                val alt = try {
                    ComponentQuery.newQuery(INTERFACE_INDEX)
                        .itemId(item.itemId)
                        .results()
                        .firstOrNull { c ->
                            val o = try { c.options } catch (_: Throwable) { null }
                            o != null && o.any { it?.contains("Deposit", true) == true } && !o.any { it?.contains("Withdraw", true) == true }
                        }
                } catch (_: Throwable) { null }
                if (alt != null) {
                    logger.info("[Bank] deposit(query): retargeted to deposit-capable component for itemId={} (compId={})", item.itemId, alt.componentId)
                    item = alt
                }
            }
        }
        val ok = deposit(script, item, option)
        logger.info("[Bank] deposit(query, option={}): end -> {}", option, ok)
        return ok
    }


    /**
     * Deposits with a [ComponentQuery] using “Deposit-All/Deposit-1” semantics (option 1).
     * @return true if interaction was sent
     */
    fun depositAll(script: PermissiveScript, query: ComponentQuery): Boolean {
        logger.info("[Bank] depositAll(query): begin (isOpen={})", isOpen())
        val rs = query.results()
        logger.info("[Bank] depositAll(query): result size={}", rs.size())
        if (rs.size() == 0) {
            val probe = ComponentQuery.newQuery(INTERFACE_INDEX).results()
            logger.info("[Bank] depositAll(query): iface {} total comps={} (sample up to 5 ids={})", INTERFACE_INDEX, probe.size(), probe.stream().limit(5).map { it.componentId }.toList())
        }
        var item = rs.first()
        if (item != null) {
            val opts = try { item.options } catch (_: Throwable) { null }
            val hasDeposit = opts != null && opts.any { it?.contains("Deposit", true) == true }
            val hasWithdraw = opts != null && opts.any { it?.contains("Withdraw", true) == true }
            if (!hasDeposit || hasWithdraw) {
                val alt = try {
                    ComponentQuery.newQuery(INTERFACE_INDEX)
                        .itemId(item.itemId)
                        .results()
                        .firstOrNull { c ->
                            val o = try { c.options } catch (_: Throwable) { null }
                            o != null && o.any { it?.contains("Deposit", true) == true } && !o.any { it?.contains("Withdraw", true) == true }
                        }
                } catch (_: Throwable) { null }
                if (alt != null) {
                    logger.info("[Bank] depositAll(query): retargeted to deposit-capable component for itemId={} (compId={})", item.itemId, alt.componentId)
                    item = alt
                }
            }
        }
        val ok = deposit(script, item, 1)
        logger.info("[Bank] depositAll(query): end -> {}", ok)
        return ok
    }


    /**
     * Low-level deposit on a specific bank component, preferring textual options when present.
     *
     * When [Component.options] include strings, this prefers “Deposit-All” then “Deposit”.
     * Otherwise falls back to numeric [option].
     *
     * @param script logging + delay helper
     * @param comp target bank component; null-safe (returns false)
     * @param option numeric option index to use when no textual actions are present
     * @return true if the interaction was issued
     */
    fun deposit(script: PermissiveScript, comp: Component?, option: Int): Boolean {
        logger.info("[Bank] deposit(comp, option={}): begin", option)
        setTransferOption(TransferOptionType.ALL)
        if (comp == null) {
            logger.warn("[Bank] deposit(comp, option={}): component is null", option)
            return false
        }
        val opts = try { comp.options } catch (_: Throwable) { null }
        logger.info("[Bank] deposit(comp, option={}): compId={}, opts={} ", option, comp.componentId, opts)
        val ok = run {
            val chosen = opts?.let {
                // Prefer an option that contains both Deposit and All, else any Deposit option
                it.firstOrNull { s -> s?.contains("Deposit", true) == true && s?.contains("All", true) == true }
                    ?: it.firstOrNull { s -> s?.contains("Deposit", true) == true }
            }
            if (chosen != null) {
                logger.info("[Bank] deposit(comp): using action '{}'", chosen)
                comp.interact(chosen) > 0
            } else {
                logger.info("[Bank] deposit(comp): using option index {}", option)
                comp.interact(option) > 0
            }
        }
        logger.info("[Bank] deposit(comp, option={}): interact -> {}", option, ok)
        if (ok) script.delay(Rand.nextInt(1, 2))
        return ok
    }

    /** Deposits all items whose names exactly match any of the provided [itemNames]. */
    /**
     * Deposits all items whose names match any of [itemNames] exactly.
     *
     * @param script logging + delay helper
     * @param itemNames exact item names to deposit
     * @return true if at least one id was resolved and all deposits succeeded
     */
    fun depositAll(script: PermissiveScript, vararg itemNames: String): Boolean {
        logger.info("[Bank] depositAll(names): begin names={} ", Arrays.toString(itemNames))
        val ids = InventoryItemQuery.newQuery(93).name(*itemNames).results().stream()
            .map { it.id }.distinct().toList()
        logger.info("[Bank] depositAll(names): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        for (id in ids) {
            var ok = depositAll(script, ComponentQuery.newQuery(INTERFACE_INDEX).itemId(id).option("Deposit-All", "Deposit-1"))
            if (!ok) {
                logger.info("[Bank] depositAll(names): bank component not found; fallback to backpack for id={}", id)
                ok = Backpack.interact("Deposit-All", id)
                        || Backpack.interact("Deposit-all", id)
                        || Backpack.interact("Deposit all", id)
                        || Backpack.interact("Deposit-1", id)
                        || Backpack.interact("Deposit 1", id)
            }
            logger.info("[Bank] depositAll(names): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] depositAll(names): end -> {}", allOk && ids.isNotEmpty())
        return allOk && ids.isNotEmpty()
    }

    /** Deposits all items whose ids match any of the provided [itemIds]. */
    /** Deposits all items whose ids match any of [itemIds]. */
    fun depositAll(script: PermissiveScript, vararg itemIds: Int): Boolean {
        logger.info("[Bank] depositAll(ids): begin ids={} ", Arrays.toString(itemIds))
        val ids = InventoryItemQuery.newQuery(93).id(*itemIds).results().stream()
            .map { it.id }.distinct().toList()
        logger.info("[Bank] depositAll(ids): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        for (id in ids) {
            var ok = depositAll(script, ComponentQuery.newQuery(INTERFACE_INDEX).itemId(id).option("Deposit-All", "Deposit-1"))
            if (!ok) {
                logger.info("[Bank] depositAll(ids): bank component not found; fallback to backpack for id={}", id)
                ok = Backpack.interact("Deposit-All", id)
                        || Backpack.interact("Deposit-all", id)
                        || Backpack.interact("Deposit all", id)
                        || Backpack.interact("Deposit-1", id)
                        || Backpack.interact("Deposit 1", id)
            }
            logger.info("[Bank] depositAll(ids): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] depositAll(ids): end -> {}", allOk && ids.isNotEmpty())
        return allOk && ids.isNotEmpty()
    }

    /** Deposits all items whose names match any of the provided regex [patterns]. */
    /** Deposits all items whose names match any of the regex [patterns]. */
    fun depositAll(script: PermissiveScript, vararg patterns: Pattern): Boolean {
        logger.info("[Bank] depositAll(patterns): begin patternsCount={}", patterns.size)
        val ids = InventoryItemQuery.newQuery(93).name(*patterns).results().stream()
            .map { it.id }.distinct().toList()
        logger.info("[Bank] depositAll(patterns): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        for (id in ids) {
            var ok = depositAll(script, ComponentQuery.newQuery(INTERFACE_INDEX).itemId(id).option("Deposit-All", "Deposit-1"))
            if (!ok) {
                logger.info("[Bank] depositAll(patterns): bank component not found; fallback to backpack for id={}", id)
                ok = Backpack.interact("Deposit-All", id)
                        || Backpack.interact("Deposit-all", id)
                        || Backpack.interact("Deposit all", id)
                        || Backpack.interact("Deposit-1", id)
                        || Backpack.interact("Deposit 1", id)
            }
            logger.info("[Bank] depositAll(patterns): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] depositAll(patterns): end -> {}", allOk && ids.isNotEmpty())
        return allOk && ids.isNotEmpty()
    }



    // Empty-Box helpers (e.g., "Empty - logs and bird's nests") within the Bank interface
    fun emptyBox(option: String): Boolean {
        if (!isOpen()) {
            logger.info("[Bank] emptyBox(option='{}'): bank not open", option)
            return false
        }
        val comp = ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
        val ok = comp != null && (comp.interact(1) > 0 || comp.interact(option) > 0)
        logger.info(
            "[Bank] emptyBox(option='{}'): component {} -> {}",
            option,
            if (comp != null) "found" else "not-found",
            ok
        )
        return ok
    }

    fun emptyBox(script: PermissiveScript, option: String): Boolean {
        val ok = emptyBox(option)
        if (ok) script.delay(Rand.nextInt(1, 2))
        return ok
    }

    suspend fun emptyBox(script: SuspendableScript, option: String): Boolean {
        val ok = emptyBox(option)
        if (ok) script.awaitTicks(2)
        return ok
    }

    // Overloads that specify the box name and fall back to backpack interaction when needed
    fun emptyBox(boxName: String, option: String): Boolean {
        if (!isOpen()) {
            logger.info("[Bank] emptyBox(name='{}', option='{}'): bank not open", boxName, option)
            return false
        }
        try {
            // 1) Target the specific item-id component inside the bank interface (most reliable)
            val bpItem = Backpack.getItem({ n, h -> h.toString().contains(n, true) }, boxName)
            logger.info(
                "[Bank] emptyBox(name='{}', option='{}'): backpack item -> {}",
                boxName,
                option,
                bpItem?.let { "${it.name} (${it.id})" } ?: "null"
            )
            if (bpItem != null) {
                val comp = ComponentQuery.newQuery(INTERFACE_INDEX).id(COMPONENT_INDEX).itemId(bpItem.id).results().firstOrNull()
                logger.debug(
                    "[Bank] emptyBox(name='{}'): bank component by itemId {}",
                    boxName,
                    if (comp != null) "found" else "not-found"
                )
                if (comp != null) {
                    val ok = comp.interact(1) > 0 || comp.interact(option) > 0
                    logger.info("[Bank] emptyBox(name='{}'): interact component by id -> {}", boxName, ok)
                    if (ok) return true
                }
            }
            // 2) Try any component with the given option under the bank interface (exact)
            val anyComp = ComponentQuery.newQuery(INTERFACE_INDEX).option(option).results().firstOrNull()
            logger.debug(
                "[Bank] emptyBox(name='{}'): bank component by option '{}' {}",
                boxName,
                option,
                if (anyComp != null) "found" else "not-found"
            )
            if (anyComp != null && (anyComp.interact(1) > 0 || anyComp.interact(option) > 0)) {
                logger.info("[Bank] emptyBox(name='{}'): interact component by option -> true", boxName)
                return true
            }
            // 2b) Fuzzy search: any component whose options contain an 'empty' action
            val results = ComponentQuery.newQuery(INTERFACE_INDEX).results()
            var fuzzyTried = 0
            var fuzzyOk = false
            results.stream().forEach { comp ->
                try {
                    val opts = comp.options
                    if (opts != null) {
                        for (op in opts) {
                            if (op != null) {
                                val s = op.lowercase()
                                if (s.contains("empty") && (s.contains("log") || s.contains("nest") || s.contains("box"))) {
                                    fuzzyTried++
                                    if (comp.interact(op) > 0) {
                                        logger.info("[Bank] emptyBox(name='{}'): fuzzy component option '{}' -> true", boxName, op)
                                        fuzzyOk = true
                                        return@forEach
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Throwable) { }
            }
            logger.debug("[Bank] emptyBox(name='{}'): fuzzy options tried {} -> {}", boxName, fuzzyTried, fuzzyOk)
            if (fuzzyOk) return true
            // 3) Last-resort: interact with the backpack item directly using the provided option
            val ok = if (bpItem != null) Backpack.interact(bpItem, option) else false
            logger.info("[Bank] emptyBox(name='{}'): fallback backpack interact -> {}", boxName, ok)
            return ok
        } catch (t: Throwable) {
            logger.warn("[Bank] emptyBox(name='{}') exception: {}", boxName, t.message)
            return false
        }
    }

    fun emptyBox(script: PermissiveScript, boxName: String, option: String): Boolean {
        val ok = emptyBox(boxName, option)
        if (ok) script.delay(Rand.nextInt(1, 2))
        return ok
    }

    suspend fun emptyBox(script: SuspendableScript, boxName: String, option: String): Boolean {
        val ok = emptyBox(boxName, option)
        if (ok) script.awaitTicks(2)
        return ok
    }

    fun depositAllExcept(script: PermissiveScript, vararg itemNames: String): Boolean {
        val keepNames = HashSet(listOf(*itemNames))
        val keepIds = Backpack.getItems().stream()
            .filter { it.name != null && keepNames.contains(it.name) }
            .map(Item::getId)
            .collect(Collectors.toSet())

        val items = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !keepIds.contains(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())

        return !items.stream().map { i: Int -> depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(i)) }.toList().contains(false)
    }

    fun depositAllExcept(script: PermissiveScript, vararg ids: Int): Boolean {
        val idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet())
        val items = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !idSet.contains(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())
        return !items.stream().map { i: Int -> depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(i)) }.toList().contains(false)
    }

    fun depositAllExcept(script: PermissiveScript, vararg patterns: Pattern): Boolean {
        val idMap = Backpack.getItems().stream()
            .filter { i: Item -> i.name != null && Arrays.stream(patterns).anyMatch { p: Pattern -> p.matcher(i.name).matches() } }
            .collect(Collectors.toMap(Item::getId, Item::getName))
        val items = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !idMap.containsKey(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())
        return !items.stream().map { i: Int -> depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(i)) }.toList().contains(false)
    }

    fun deposit(script: PermissiveScript, itemId: Int, option: Int): Boolean =
        deposit(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(itemId), option)

    fun deposit(script: PermissiveScript, name: String, spred: BiFunction<String, CharSequence, Boolean>, option: Int): Boolean =
        deposit(script, ComponentQuery.newQuery(517).itemName(name, spred), option)

    fun deposit(script: PermissiveScript, name: String, option: Int): Boolean =
        deposit(script, name, BiFunction { a, b -> a.contentEquals(b) }, option)

    /**
     * Loads a numbered preset (1–18). For presets >= 10 the preset browsing tab must be active;
     * this method toggles it on demand before clicking the target preset.
     */
    /**
     * Loads a numbered preset (1–18), toggling the preset-browsing tab for 10–18 as needed.
     *
     * @param script logging + delay helper
     * @param presetNumber 1..18 (1..9 primary tab, 10..18 browsing tab)
     * @return true if a click for the target preset was issued
     */
    fun loadPreset(script: PermissiveScript, presetNumber: Int): Boolean {
        val presetBrowsingValue = VarDomain.getVarBitValue(PRESET_BROWSING_VARBIT_ID)
        if ((presetNumber >= 10 && presetBrowsingValue < 1) || (presetNumber < 10 && presetBrowsingValue > 0)) {
            MiniMenu.doAction(Action.COMPONENT, 1, 100, 33882231)
            script.delay(Rand.nextInt(1, 2))
        }
        val result = MiniMenu.doAction(Action.COMPONENT, 1, presetNumber % 9, 33882231) > 0
        if (result) {
            previousLoadedPreset = presetNumber
        }
        return result
    }

    /** Reads a varbit value for the given bank [slot]. Returns [Int.MIN_VALUE] if unavailable. */
    fun getVarbitValue(slot: Int, varbitId: Int): Int {
        val inventory: Inventory? = getInventory()
        return inventory?.getVarbitValue(slot, varbitId) ?: Int.MIN_VALUE
    }

    /** Ensures the current transfer option matches [transferoptionType], clicking if necessary. */
    fun setTransferOption(transferoptionType: TransferOptionType): Boolean {
        val depositOptionState = VarDomain.getVarBitValue(WITHDRAW_TYPE_VARBIT_ID)
        return depositOptionState == transferoptionType.varbitStateValue ||
                MiniMenu.doAction(Action.COMPONENT, 1, -1, 33882215) > 0
    }

    /** Returns the last successfully loaded preset index for this session. */
    fun getPreviousLoadedPreset(): Int = previousLoadedPreset

    // Suspendable overloads for coroutine-based scripts

    /**
     * Opens the bank and awaits the interface becoming visible.
     *
     * @param script coroutine context with await utilities
     * @return true if the bank is open after waiting
     */
    suspend fun open(script: SuspendableScript): Boolean {
        val opened = open(object : PermissiveScript() {})
        if (opened) script.awaitUntil(10) { isOpen() }
        return isOpen()
    }


    suspend fun close(script: SuspendableScript): Boolean {
        val res = close()
        if (res) script.awaitUntil(5) { !isOpen() }
        return !isOpen()
    }


    suspend fun loadLastPreset(script: SuspendableScript): Boolean {
        val res = loadLastPreset()
        if (res) script.awaitTicks(2)
        return res
    }


    suspend fun loadPreset(script: SuspendableScript, presetNumber: Int): Boolean {
        val res = loadPreset(object : PermissiveScript() {}, presetNumber)
        if (res) script.awaitTicks(2)
        return res
    }


    /**
     * Deposits carried items, then yields for ~1 tick to pace interactions in coroutines.
     * @return true if the deposit action was issued
     */
    suspend fun depositAll(script: SuspendableScript): Boolean {
        logger.info("[Bank] suspend depositAll(): begin")
        val res = depositAll()
        logger.info("[Bank] suspend depositAll(): base call -> {}", res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend depositAll(): end -> {}", res)
        return res
    }


    /** Suspendable counterpart to [deposit] that yields briefly on success. */
    suspend fun deposit(script: SuspendableScript, comp: Component?, option: Int): Boolean {
        logger.info("[Bank] suspend deposit(comp, option={}): begin compId={}", option, comp?.componentId)
        setTransferOption(TransferOptionType.ALL)
        val ok = comp != null && comp.interact(option) > 0
        logger.info("[Bank] suspend deposit(comp, option={}): interact -> {}", option, ok)
        if (ok) script.awaitTicks(1)
        logger.info("[Bank] suspend deposit(comp, option={}): end -> {}", option, ok)
        return ok
    }


    /** Suspendable convenience variant of [depositAll] using a query. */
    /** Suspendable counterpart to [depositAll] by query. */
    suspend fun depositAll(script: SuspendableScript, query: ComponentQuery): Boolean {
        logger.info("[Bank] suspend depositAll(query): begin (isOpen={})", isOpen())
        val rs = query.results()
        logger.info("[Bank] suspend depositAll(query): result size={}", rs.size())
        if (rs.size() == 0) {
            val probe = ComponentQuery.newQuery(INTERFACE_INDEX).results()
            logger.info("[Bank] suspend depositAll(query): iface {} total comps={} (sample up to 5 ids={})", INTERFACE_INDEX, probe.size(), probe.stream().limit(5).map { it.componentId }.toList())
        }
        val item = rs.first()
        val ok = deposit(script, item, 1)
        logger.info("[Bank] suspend depositAll(query): end -> {}", ok)
        return ok
    }


    /** Suspendable counterpart to [depositAll] by names. */
    suspend fun depositAll(script: SuspendableScript, vararg itemNames: String): Boolean {
        logger.info("[Bank] suspend depositAll(names): begin names={}", Arrays.toString(itemNames))
        val ids = InventoryItemQuery.newQuery(93).name(*itemNames).results().stream()
            .map { it.id }.distinct().toList()
        logger.info("[Bank] suspend depositAll(names): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        for (id in ids) {
            var ok = depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(id))
            if (!ok) {
                logger.info("[Bank] suspend depositAll(names): bank component not found; fallback to backpack for id={}", id)
                ok = Backpack.interact("Deposit-All", id) || Backpack.interact("Deposit-1", id)
                if (ok) script.awaitTicks(1)
            }
            logger.info("[Bank] suspend depositAll(names): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAll(names): end -> {}", allOk && ids.isNotEmpty())
        return allOk && ids.isNotEmpty()
    }


    /** Suspendable counterpart to [depositAll] by ids. */
    suspend fun depositAll(script: SuspendableScript, vararg itemIds: Int): Boolean {
        logger.info("[Bank] suspend depositAll(ids): begin ids={}", Arrays.toString(itemIds))
        val ids = InventoryItemQuery.newQuery(93).id(*itemIds).results().stream()
            .map { it.id }.distinct().toList()
        logger.info("[Bank] suspend depositAll(ids): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        for (id in ids) {
            var ok = depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(id))
            if (!ok) {
                logger.info("[Bank] suspend depositAll(ids): bank component not found; fallback to backpack for id={}", id)
                ok = Backpack.interact("Deposit-All", id) || Backpack.interact("Deposit-1", id)
                if (ok) script.awaitTicks(1)
            }
            logger.info("[Bank] suspend depositAll(ids): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAll(ids): end -> {}", allOk && ids.isNotEmpty())
        return allOk && ids.isNotEmpty()
    }


    /** Suspendable counterpart to [depositAll] by regex patterns. */
    suspend fun depositAll(script: SuspendableScript, vararg patterns: Pattern): Boolean {
        logger.info("[Bank] suspend depositAll(patterns): begin patternsCount={}", patterns.size)
        // Resolve by backpack item ids using ConfigManager names (backpack item.name may be blank while bank is open)
        val ids = try {
            val inv = Backpack.getInventory()
            val items = inv?.items ?: emptyList()
            items.stream()
                .filter { it.id != -1 }
                .filter { i ->
                    val name = try { net.botwithus.rs3.cache.assets.ConfigManager.getItemProvider().provide(i.id).name } catch (_: Throwable) { "" }
                    patterns.any { p -> p.matcher(name ?: "").find() }
                }
                .map { it.id }
                .distinct()
                .toList()
        } catch (_: Throwable) { emptyList<Int>() }
        logger.info("[Bank] suspend depositAll(patterns): resolved ids={} (count={})", ids, ids.size)
        var allOk = true
        if (ids.isNotEmpty()) {
            for (id in ids) {
                var ok = depositAll(script, ComponentQuery.newQuery(INTERFACE_INDEX).itemId(id))
                if (!ok) {
                    logger.info("[Bank] suspend depositAll(patterns): bank component not found; fallback to backpack for id={}", id)
                    ok = Backpack.interact("Deposit-All", id)
                            || Backpack.interact("Deposit-all", id)
                            || Backpack.interact("Deposit all", id)
                            || Backpack.interact("Deposit-1", id)
                            || Backpack.interact("Deposit 1", id)
                    if (ok) script.awaitTicks(1)
                }
                logger.info("[Bank] suspend depositAll(patterns): id={} -> {}", id, ok)
                allOk = allOk && ok
            }
            logger.info("[Bank] suspend depositAll(patterns): end -> {}", allOk)
            return allOk
        }

        // Heuristic fallback for common log item ids when names are unavailable
        val logsPatternRequested = patterns.any { it.pattern().contains("logs", ignoreCase = true) }
        if (logsPatternRequested) {
            val commonLogIds = setOf(1511, 1513, 1515, 1517, 1519, 1521) // normal, magic, yew, maple, willow, oak (OSRS/RS3 common ids)
            val inv = try { Backpack.getInventory() } catch (_: Throwable) { null }
            val items = inv?.items ?: emptyList()
            val fallbackIds = items.map { it.id }.filter { it in commonLogIds }.distinct()
            logger.info("[Bank] suspend depositAll(patterns): logs-heuristic ids={} (count={})", fallbackIds, fallbackIds.size)
            if (fallbackIds.isNotEmpty()) {
                for (id in fallbackIds) {
                    var ok = depositAll(script, ComponentQuery.newQuery(INTERFACE_INDEX).itemId(id))
                    if (!ok) {
                        ok = Backpack.interact("Deposit-All", id)
                                || Backpack.interact("Deposit-all", id)
                                || Backpack.interact("Deposit all", id)
                                || Backpack.interact("Deposit-1", id)
                                || Backpack.interact("Deposit 1", id)
                        if (ok) script.awaitTicks(1)
                    }
                    logger.info("[Bank] suspend depositAll(patterns): logs-heuristic id={} -> {}", id, ok)
                    allOk = allOk && ok
                }
                logger.info("[Bank] suspend depositAll(patterns): end (logs-heuristic) -> {}", allOk)
                return allOk
            }
        }

        // Diagnostics: log a small sample of backpack item ids->names when no ids resolved
        try {
            val inv = Backpack.getInventory()
            val items = inv?.items ?: emptyList()
            val samples = items.filter { it.id != -1 }
                .map {
                    val nm = try { net.botwithus.rs3.cache.assets.ConfigManager.getItemProvider().provide(it.id).name } catch (_: Throwable) { "" }
                    "${it.id}:${nm}"
                }
                .take(10)
            logger.info("[Bank] suspend depositAll(patterns): backpack sample (id:name) -> {}", samples)
        } catch (_: Throwable) { }

        // Fallback: derive targets directly from bank interface components by itemName
        val bankTargets = try {
            ComponentQuery.newQuery(INTERFACE_INDEX)
                .results()
                .toList()
                .filter { c ->
                    val opts = try { c.options } catch (_: Throwable) { null }
                    val depositable = opts != null && opts.any { it?.contains("Deposit", true) == true } && !opts.any { it?.contains("Withdraw", true) == true }
                    if (!depositable) return@filter false
                    val itemName = try {
                        net.botwithus.rs3.cache.assets.ConfigManager.getItemProvider().provide(c.itemId).name
                    } catch (_: Throwable) { null }
                    val nameMatches = itemName != null && patterns.any { p -> p.matcher(itemName).find() }
                    val logsHeuristic = logsPatternRequested && c.itemId in setOf(1511, 1513, 1515, 1517, 1519, 1521)
                    nameMatches || logsHeuristic
                }
        } catch (_: Throwable) { emptyList() }

        logger.info("[Bank] suspend depositAll(patterns): bank-fallback targets={} ", bankTargets.size)
        if (bankTargets.isEmpty()) {
            // Log a compact summary of any deposit-capable comps for debugging
            try {
                val all = ComponentQuery.newQuery(INTERFACE_INDEX).results().toList()
                val depositables = all.filter { c ->
                    val o = try { c.options } catch (_: Throwable) { null }
                    o != null && o.any { it?.contains("Deposit", true) == true }
                }
                val sample = depositables.take(10).map { c ->
                    val nm = try { net.botwithus.rs3.cache.assets.ConfigManager.getItemProvider().provide(c.itemId).name } catch (_: Throwable) { "" }
                    val opts = try { c.options } catch (_: Throwable) { null }
                    "${c.componentId}:${c.itemId}:${nm}:${opts}"
                }
                logger.info("[Bank] suspend depositAll(patterns): depositable comps sample compId:itemId:name:options -> {}", sample)
            } catch (_: Throwable) { }
        }
        for (comp in bankTargets) {
            val ok = deposit(script, comp, 1)
            logger.info("[Bank] suspend depositAll(patterns): bank-fallback compId={} -> {}", comp.componentId, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAll(patterns): end (bank-fallback) -> {}", allOk && bankTargets.isNotEmpty())
        return allOk && bankTargets.isNotEmpty()
    }

    /**
     * Deposits everything except exact names and yields briefly.
     *
     * @param script coroutine context with await utilities
     * @param itemNames exact item names to keep
     * @return true if there were targets and all deposits succeeded
     */
    suspend fun depositAllExcept(script: SuspendableScript, vararg itemNames: String): Boolean {
        logger.info("[Bank] suspend depositAllExcept(names): begin names={}", Arrays.toString(itemNames))
        val keepNames = HashSet(listOf(*itemNames))
        val keepIds = Backpack.getItems().stream()
            .filter { it.name != null && keepNames.contains(it.name) }
            .map(Item::getId)
            .collect(Collectors.toSet())
        logger.info("[Bank] suspend depositAllExcept(names): keepIds={} (count={})", keepIds, keepIds.size)
        val itemIds = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !keepIds.contains(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())
        logger.info("[Bank] suspend depositAllExcept(names): targetIds={} (count={})", itemIds, itemIds.size)
        var allOk = true
        for (id in itemIds) {
            val ok = depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(id))
            logger.info("[Bank] suspend depositAllExcept(names): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAllExcept(names): end -> {}", allOk && itemIds.isNotEmpty())
        return allOk && itemIds.isNotEmpty()
    }


    suspend fun depositAllExcept(script: SuspendableScript, vararg ids: Int): Boolean {
        logger.info("[Bank] suspend depositAllExcept(ids): begin ids={}", Arrays.toString(ids))
        val idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet())
        val itemIds = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !idSet.contains(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())
        logger.info("[Bank] suspend depositAllExcept(ids): targetIds={} (count={})", itemIds, itemIds.size)
        var allOk = true
        for (id in itemIds) {
            val ok = depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(id))
            logger.info("[Bank] suspend depositAllExcept(ids): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAllExcept(ids): end -> {}", allOk && itemIds.isNotEmpty())
        return allOk && itemIds.isNotEmpty()
    }


    suspend fun depositAllExcept(script: SuspendableScript, vararg patterns: Pattern): Boolean {
        logger.info("[Bank] suspend depositAllExcept(patterns): begin patternsCount={}", patterns.size)
        val idMap = Backpack.getItems().stream()
            .filter { i: Item -> i.name != null && Arrays.stream(patterns).anyMatch { p: Pattern -> p.matcher(i.name).matches() } }
            .collect(Collectors.toMap(Item::getId, Item::getName))
        val itemIds = ComponentQuery.newQuery(517).results().stream()
            .filter { i -> !idMap.containsKey(i.itemId) && (i.options.contains("Deposit-All") || i.options.contains("Deposit-1")) }
            .map(Component::getItemId)
            .collect(Collectors.toSet())
        logger.info("[Bank] suspend depositAllExcept(patterns): targetIds={} (count={})", itemIds, itemIds.size)
        var allOk = true
        for (id in itemIds) {
            val ok = depositAll(script, ComponentQuery.newQuery(517).id(COMPONENT_INDEX).itemId(id))
            logger.info("[Bank] suspend depositAllExcept(patterns): id={} -> {}", id, ok)
            allOk = allOk && ok
        }
        logger.info("[Bank] suspend depositAllExcept(patterns): end -> {}", allOk && itemIds.isNotEmpty())
        return allOk && itemIds.isNotEmpty()
    }


    suspend fun depositEquipment(script: SuspendableScript): Boolean {
        logger.info("[Bank] suspend depositEquipment(): begin")
        val res = depositEquipment()
        logger.info("[Bank] suspend depositEquipment(): base call -> {}", res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend depositEquipment(): end -> {}", res)
        return res
    }


    suspend fun depositBackpack(script: SuspendableScript): Boolean {
        logger.info("[Bank] suspend depositBackpack(): begin")
        val res = depositBackpack()
        logger.info("[Bank] suspend depositBackpack(): base call -> {}", res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend depositBackpack(): end -> {}", res)
        return res
    }


    suspend fun withdrawAll(script: SuspendableScript, name: String): Boolean {
        logger.info("[Bank] suspend withdrawAll(name='{}'): begin", name)
        val res = withdrawAll(name)
        logger.info("[Bank] suspend withdrawAll(name='{}'): base call -> {}", name, res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend withdrawAll(name='{}'): end -> {}", name, res)
        return res
    }


    suspend fun withdrawAll(script: SuspendableScript, id: Int): Boolean {
        logger.info("[Bank] suspend withdrawAll(id={}): begin", id)
        val res = withdrawAll(id)
        logger.info("[Bank] suspend withdrawAll(id={}): base call -> {}", id, res)
        if (res) script.awaitTicks(1)
        logger.info("[Bank] suspend withdrawAll(id={}): end -> {}", id, res)
        return res
    }

}
