package plutoproject.framework.common.util.chat.component

import net.kyori.adventure.text.Component

fun Iterable<Component>.replace(pattern: String, str: String): Iterable<Component> {
    return map { it.replace(pattern, str) }
}

fun Iterable<Component>.replace(pattern: String, component: Component): Iterable<Component> {
    return map { it.replace(pattern, component) }
}

fun Iterable<Component>.replace(pattern: String, any: Any?): Iterable<Component> {
    return map { it.replace(pattern, any) }
}
