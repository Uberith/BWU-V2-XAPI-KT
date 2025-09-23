package net.botwithus.kxapi.util

import net.botwithus.rs3.interfaces.Component
import net.botwithus.rs3.minimenu.Interactive
import net.botwithus.xapi.query.ComponentQuery
import java.util.regex.Pattern

private inline fun Interactive.interactBy(match: (String) -> Boolean): Boolean {
    options.forEachIndexed { index, value ->
        if (value != null && match(value)) return interact(index) != 0
    }
    return false
}

fun Interactive.interactWith(option: String): Boolean = interactBy { it == option }

fun Interactive.interactWith(pattern: Pattern): Boolean = interactBy { pattern.matcher(it).matches() }

fun Interactive.interactWith(predicate: (String) -> Boolean): Boolean = interactBy(predicate)

fun ComponentQuery.hasOption(option: String): ComponentQuery = option(option)
fun ComponentQuery.isVisible(): ComponentQuery = hidden(false)

fun ComponentQuery.subComponentIndex(index: Int): Component? = 
    results().firstOrNull()?.children?.getOrNull(index)

fun ComponentQuery.findChild(predicate: (Component) -> Boolean): Component? =
    results().flatMap { it.children }.firstOrNull(predicate)

fun ComponentQuery.findByText(text: String): Component? =
    findChild { it.text == text }

fun ComponentQuery.findByContainsText(text: String): Component? =
    findChild { it.text.contains(text) }

fun ComponentQuery.findByOption(option: String): Component? =
    findChild { it.options.contains(option) }

fun ComponentQuery.findBySubComponentId(subComponentId: Int): Component? =
    findChild { it.subComponentId == subComponentId }

fun ComponentQuery.componentIndex(vararg ids: Int): ComponentQuery = this.id(*ids)

fun Component.hasOption(option: String): Boolean = options.contains(option)

fun Component.interactWithOption(option: String): Boolean =
    if (hasOption(option)) interact(option) > 0 else false

fun ComponentQuery.selectItemByName(itemName: String, itemNameProvider: (Int) -> String): Boolean {
    val targetChild = findChild { child -> itemNameProvider(child.itemId) == itemName }
    if (targetChild != null) {
        val buttonId = targetChild.subComponentId - 1
        return findBySubComponentId(buttonId)?.interactWithOption("Select") == true
    }
    return false
}

fun ComponentQuery.interactWithFirstOption(option: String): Boolean =
    findByOption(option)?.interactWithOption(option) == true

fun ComponentQuery.hasResults(): Boolean = results().size() > 0
fun ComponentQuery.isEmpty(): Boolean = results().size() == 0
