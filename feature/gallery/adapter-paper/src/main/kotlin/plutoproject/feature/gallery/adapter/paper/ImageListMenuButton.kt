package plutoproject.feature.gallery.adapter.paper

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.gallery.adapter.paper.screen.ImageListScreen
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val ImageListMenuButtonDescriptor = ButtonDescriptor {
    id = "gallery:image_list"
}

@Composable
@Suppress("FunctionName")
fun ImageListMenuButton() {
    val navigator = LocalNavigator.currentOrThrow

    Item(
        material = Material.GLOW_ITEM_FRAME,
        name = IMAGE_LIST_MENU_BUTTON,
        lore = buildList {
            add(IMAGE_LIST_MENU_BUTTON_LORE_DESC)
            add(Component.empty())
            add(IMAGE_LIST_MENU_BUTTON_LORE_OPERATION)
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(ImageListScreen())
        }
    )
}
