package plutoproject.framework.paper.api.interactive.canvas.dialog.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody
import org.bukkit.inventory.ItemStack

@Composable
@Suppress("UnstableApiUsage")
fun ItemBody(
    item: ItemStack,
    description: PlainMessageDialogBody? = null,
    showDecorations: Boolean = true,
    showTooltip: Boolean = true,
    width: Int = 16,
    height: Int = 16,
) {
    BodyElement(remember(item, description, showDecorations, showTooltip, width, height) {
        DialogBody.item(item)
            .description(description)
            .showDecorations(showDecorations)
            .showTooltip(showTooltip)
            .width(width)
            .height(height)
            .build()
    })
}
