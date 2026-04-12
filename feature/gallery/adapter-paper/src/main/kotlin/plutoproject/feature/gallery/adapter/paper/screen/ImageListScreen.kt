package plutoproject.feature.gallery.adapter.paper.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_OPERATION
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES_HINT
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_UNAVAILABLE
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD_HINT
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_LORE_OPERATION
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_LORE_SIZE
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_LORE_TYPE
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_NAME
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_TYPE_ANIMATED
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_ENTRY_TYPE_STATIC
import plutoproject.feature.gallery.adapter.paper.IMAGE_LIST_TITLE
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_HEIGHT
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_NAME
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_TYPE
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_WIDTH
import plutoproject.feature.gallery.adapter.paper.deleteOwnedImage
import plutoproject.feature.gallery.adapter.paper.hasReachedImageLimit
import plutoproject.feature.gallery.adapter.paper.hasUnfinishedImageCreateSession
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.layout.list.ListMenu
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import net.kyori.adventure.text.Component

class ImageListScreen : ListMenu<Image, ImageListScreenModel>() {
    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = IMAGE_LIST_TITLE
        super.MenuLayout()
    }

    @Composable
    override fun BottomBorderAttachment() {
        if (LocalListMenuModel.current.isLoading) return
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            PreviousTurner()
            Create()
            NextTurner()
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun Create() {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        val imageConfig = remember { plutoproject.feature.gallery.adapter.common.koin.get<plutoproject.feature.gallery.adapter.common.GalleryConfig>().image }
        val model = LocalListMenuModel.current
        var hasUnfinishedUpload by remember { mutableStateOf(false) }
        var hasReachedLimit by remember { mutableStateOf(false) }

        LaunchedEffect(navigator.items.size, model.contents.size, model.page, model.pageCount) {
            hasUnfinishedUpload = hasUnfinishedImageCreateSession(player.uniqueId)
            hasReachedLimit = hasReachedImageLimit(player.uniqueId)
        }

        Item(
            material = Material.GLOW_ITEM_FRAME,
            name = IMAGE_LIST_CREATE,
            lore = buildList {
                add(Component.empty())
                add(IMAGE_LIST_CREATE_LORE_OPERATION)
                if (hasUnfinishedUpload) {
                    add(IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD)
                    add(IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD_HINT)
                }
                if (hasReachedLimit) {
                    add(IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES)
                    add(
                        IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES_HINT
                            .replace(IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER, imageConfig.maxImagesPerPlayer)
                    )
                }
                if (!hasUnfinishedUpload && !hasReachedLimit) {
                    add(IMAGE_LIST_CREATE_LORE_UNAVAILABLE)
                }
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                if (hasUnfinishedUpload || hasReachedLimit) {
                    player.playSound(UI_FAILED_SOUND)
                    return@clickable
                }
                navigator.push(ImageCreateScreen())
            }
        )
    }

    @Composable
    override fun modelProvider(): ImageListScreenModel {
        val player = LocalPlayer.current
        return ImageListScreenModel(player.uniqueId)
    }

    @Composable
    override fun Element(obj: Image) {
        val player = LocalPlayer.current
        val model = LocalListMenuModel.current
        val coroutineScope = rememberCoroutineScope()
        val imageType = when (obj.type) {
            ImageType.STATIC -> IMAGE_LIST_ENTRY_TYPE_STATIC
            ImageType.ANIMATED -> IMAGE_LIST_ENTRY_TYPE_ANIMATED
        }
        Item(
            material = Material.PAPER,
            name = IMAGE_LIST_ENTRY_NAME.replace(IMAGE_PLACEHOLDER_NAME, obj.name),
            lore = listOf(
                IMAGE_LIST_ENTRY_LORE_TYPE.replace(IMAGE_PLACEHOLDER_TYPE, imageType),
                IMAGE_LIST_ENTRY_LORE_SIZE
                    .replace(IMAGE_PLACEHOLDER_WIDTH, obj.widthBlocks)
                    .replace(IMAGE_PLACEHOLDER_HEIGHT, obj.heightBlocks),
                Component.empty(),
                IMAGE_LIST_ENTRY_LORE_OPERATION
            ),
            modifier = Modifier.clickable {
                if (clickType != ClickType.RIGHT) return@clickable
                coroutineScope.launch {
                    if (deleteOwnedImage(player.uniqueId, obj.id)) {
                        player.playSound(UI_SUCCEED_SOUND)
                        model.loadPageContents()
                    }
                }
            }
        )
    }
}
