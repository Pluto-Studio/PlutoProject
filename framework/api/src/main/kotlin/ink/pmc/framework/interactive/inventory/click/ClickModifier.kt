package ink.pmc.framework.interactive.inventory.click

import ink.pmc.framework.interactive.inventory.Modifier

open class ClickModifier(
    val merged: Boolean = false,
    val cancelClickEvent: Boolean,
    val onClick: suspend (ClickScope.() -> Unit),
) : Modifier.Element<ClickModifier> {
    override fun mergeWith(other: ClickModifier) = ClickModifier(
        merged = true,
        cancelClickEvent = cancelClickEvent || other.cancelClickEvent,
        onClick = {
            if (!other.merged)
                onClick()
            other.onClick(this)
        },
    )
}

fun Modifier.clickable(
    cancelClickEvent: Boolean = true,
    onClick: suspend ClickScope.() -> Unit
) =
    then(
        ClickModifier(
            cancelClickEvent = cancelClickEvent,
            onClick = onClick,
        )
    )