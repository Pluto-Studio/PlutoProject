package plutoproject.foundation.common.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title

fun Title.replaceSubTitle(pattern: String, str: String): Title =
    Title.title(title(), subtitle().replace(pattern, str), times())

fun Title.replaceSubTitle(pattern: String, component: Component): Title =
    Title.title(title(), subtitle().replace(pattern, component), times())

fun Title.replaceSubTitle(pattern: String, any: Any?): Title =
    Title.title(title(), subtitle().replace(pattern, any), times())
