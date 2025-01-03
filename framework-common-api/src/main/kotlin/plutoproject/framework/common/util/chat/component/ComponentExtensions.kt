package plutoproject.framework.common.util.chat.component

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.TextColor

fun Component.replace(string: String, str: String): Component {
    val replaceConfig = TextReplacementConfig.builder()
        .match(string)
        .replacement(Component.text(str))
        .build()
    return this.replaceText(replaceConfig)
}

fun Component.replace(pattern: String, component: Component): Component {
    val replaceConfig = TextReplacementConfig.builder()
        .match(pattern)
        .replacement(component)
        .build()
    return replaceText(replaceConfig)
}

fun Component.replace(string: String, any: Any?): Component {
    return replace(string, any.toString())
}

fun Component.replaceColor(original: TextColor, new: TextColor): Component {
    val updatedComponent =
        if (this.style().color() == original) {
            this.style(this.style().color(new))
        } else {
            this
        }
    return updatedComponent
        .children()
        .fold(updatedComponent.children(emptyList())) { comp, child ->
            comp.append(child.replaceColor(original, new))
        }
}

fun Component.splitLines(): Iterable<Component> {
    var curr = Component.empty()
    return buildList {
        val root = this@splitLines.children(emptyList())
        if (root != Component.empty()) {
            add(root)
        }
        children().forEach { child ->
            if (child == Component.newline()) {
                if (curr != Component.empty()) {
                    add(curr)
                }
                curr = Component.empty()
                return@forEach
            }
            curr = curr.append(child)
        }
        if (curr != Component.empty()) {
            add(curr)
        }
    }
}
