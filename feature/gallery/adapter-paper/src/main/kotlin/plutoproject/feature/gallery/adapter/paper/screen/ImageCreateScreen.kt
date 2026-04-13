package plutoproject.feature.gallery.adapter.paper.screen

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.event.ClickCallback
import plutoproject.feature.gallery.adapter.common.DitherMode
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.RepositionMode
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.paper.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.dialog.Dialog
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.NumberRangeInput
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.SingleOptionInput
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.TextInput
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.entity.clearDialog

private const val IMAGE_CREATE_NAME_INPUT_KEY = "name"
private const val IMAGE_CREATE_WIDTH_INPUT_KEY = "width"
private const val IMAGE_CREATE_HEIGHT_INPUT_KEY = "height"
private const val IMAGE_CREATE_DITHER_INPUT_KEY = "dither"
private const val IMAGE_CREATE_FILL_INPUT_KEY = "fill"

private const val IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT = "default"
private const val IMAGE_CREATE_DITHER_OPTION_ID_NONE = "none"
private const val IMAGE_CREATE_DITHER_OPTION_ID_FLOYD_STEINBERG = "floyd-steinberg"
private const val IMAGE_CREATE_DITHER_OPTION_ID_ORDERED_BAYER = "ordered-bayer"

private const val IMAGE_CREATE_FILL_OPTION_ID_CONTAIN = "contain"
private const val IMAGE_CREATE_FILL_OPTION_ID_COVER = "cover"
private const val IMAGE_CREATE_FILL_OPTION_ID_STRETCH = "stretch"

class ImageCreateScreen : InteractiveScreen() {
    @Composable
    @Suppress("UnstableApiUsage")
    override fun Content() {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        val galleryConfig = remember { koin.get<GalleryConfig>() }
        val imageConfig = remember(galleryConfig) { galleryConfig.image }
        val renderConfig = remember(galleryConfig) { galleryConfig.render }

        var name by rememberSaveable { mutableStateOf("") }
        var width by rememberSaveable { mutableStateOf(1f) }
        var height by rememberSaveable { mutableStateOf(1f) }
        var ditherOptionId by rememberSaveable { mutableStateOf(IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT) }
        var fillOptionId by rememberSaveable { mutableStateOf(renderConfig.repositionMode.toFillOptionId()) }

        val cancelCallback = DialogAction.customClick(
            { _, _ -> navigator.pop() },
            ClickCallback.Options.builder().build()
        )
        val submitCallback = DialogAction.customClick({ view, _ ->
            val submittedName = view.getText(IMAGE_CREATE_NAME_INPUT_KEY) ?: ""
            val submittedWidth = (view.getFloat(IMAGE_CREATE_WIDTH_INPUT_KEY) ?: 1f).toInt()
            val submittedHeight = (view.getFloat(IMAGE_CREATE_HEIGHT_INPUT_KEY) ?: 1f).toInt()
            val submittedDitherOptionId =
                view.getText(IMAGE_CREATE_DITHER_INPUT_KEY) ?: IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT
            val submittedFillOptionId =
                view.getText(IMAGE_CREATE_FILL_INPUT_KEY) ?: renderConfig.repositionMode.toFillOptionId()
            val mapCount = runCatching { Math.multiplyExact(submittedWidth, submittedHeight) }.getOrNull()

            name = submittedName
            width = submittedWidth.toFloat()
            height = submittedHeight.toFloat()
            ditherOptionId = submittedDitherOptionId
            fillOptionId = submittedFillOptionId

            if (submittedName.isBlank()) {
                player.playSound(UI_FAILED_SOUND)
                navigator.push(ImageCreateFeedbackScreen(IMAGE_CREATE_FAILED_EMPTY_NAME))
                return@customClick
            }

            if (submittedName.length > imageConfig.maxNameLength) {
                player.playSound(UI_FAILED_SOUND)
                navigator.push(
                    ImageCreateFeedbackScreen(
                        IMAGE_CREATE_FAILED_TOO_LONG.replace(
                            IMAGE_PLACEHOLDER_MAX_NAME_LENGTH,
                            imageConfig.maxNameLength
                        )
                    )
                )
                return@customClick
            }

            if (mapCount == null || mapCount > imageConfig.maxMapBlocks) {
                player.playSound(UI_FAILED_SOUND)
                navigator.push(
                    ImageCreateFeedbackScreen(
                        IMAGE_CREATE_FAILED_TOO_MANY_MAP_BLOCKS
                            .replace(IMAGE_PLACEHOLDER_MAX_MAP_BLOCKS, imageConfig.maxMapBlocks)
                    )
                )
                return@customClick
            }

            coroutineScope.launch {
                when (
                    submitImageCreate(
                        player = player,
                        name = submittedName,
                        width = submittedWidth,
                        height = submittedHeight,
                        repositionMode = submittedFillOptionId.toRepositionMode(),
                        ditherMode = submittedDitherOptionId.toDitherMode(renderConfig.ditherMode),
                    )
                ) {
                    ImageCreateSubmissionResult.TooManyMapBlocks -> {
                        player.playSound(UI_FAILED_SOUND)
                        navigator.push(
                            ImageCreateFeedbackScreen(
                                IMAGE_CREATE_FAILED_TOO_MANY_MAP_BLOCKS
                                    .replace(IMAGE_PLACEHOLDER_MAX_MAP_BLOCKS, imageConfig.maxMapBlocks)
                            )
                        )
                    }

                    ImageCreateSubmissionResult.TooManyImages -> {
                        player.playSound(UI_FAILED_SOUND)
                        navigator.push(
                            ImageCreateFeedbackScreen(
                                IMAGE_CREATE_FAILED_TOO_MANY_IMAGES
                                    .replace(IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER, imageConfig.maxImagesPerPlayer)
                            )
                        )
                    }

                    is ImageCreateSubmissionResult.UnfinishedSessionExists -> {
                        player.playSound(UI_FAILED_SOUND)
                        navigator.push(ImageCreateFeedbackScreen(IMAGE_CREATE_FAILED_UNFINISHED_UPLOAD))
                    }

                    is ImageCreateSubmissionResult.Created -> {
                        withContext(player.coroutineContext) {
                            player.clearDialog()
                            player.closeInventory()
                        }
                    }
                }
            }
        }, ClickCallback.Options.builder().build())

        val cancelButton = ActionButton.builder(IMAGE_CREATE_CANCEL).action(cancelCallback).build()
        val submitButton = ActionButton.builder(IMAGE_CREATE_SUBMIT).action(submitCallback).build()

        Dialog(
            type = DialogType.confirmation(submitButton, cancelButton),
            canCloseWithEscape = false,
            title = IMAGE_CREATE_TITLE,
            body = {
                // PlainMessageBody(IMAGE_CREATE_DESCRIPTION)
                // PlainMessageBody(IMAGE_CREATE_FILL_ABOUT, width = 300)
                // PlainMessageBody(IMAGE_CREATE_DITHER_ABOUT, width = 300)
            },
            input = {
                TextInput(
                    key = IMAGE_CREATE_NAME_INPUT_KEY,
                    label = IMAGE_CREATE_NAME_INPUT_LABEL,
                    initial = name,
                    maxLength = imageConfig.maxNameLength,
                    width = 300,
                )
                NumberRangeInput(
                    key = IMAGE_CREATE_WIDTH_INPUT_KEY,
                    label = IMAGE_CREATE_WIDTH_INPUT_LABEL,
                    start = 1f,
                    end = imageConfig.maxLongEdgeBlocks.toFloat(),
                    initial = width,
                    step = 1f,
                    width = 300,
                )
                NumberRangeInput(
                    key = IMAGE_CREATE_HEIGHT_INPUT_KEY,
                    label = IMAGE_CREATE_HEIGHT_INPUT_LABEL,
                    start = 1f,
                    end = imageConfig.maxLongEdgeBlocks.toFloat(),
                    initial = height,
                    step = 1f,
                    width = 300,
                )
                SingleOptionInput(
                    key = IMAGE_CREATE_FILL_INPUT_KEY,
                    label = IMAGE_CREATE_FILL_INPUT_LABEL,
                    width = 300,
                    entries = listOf(
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_FILL_OPTION_ID_CONTAIN,
                            IMAGE_CREATE_FILL_OPTION_CONTAIN_DISPLAY,
                            fillOptionId == IMAGE_CREATE_FILL_OPTION_ID_CONTAIN,
                        ),
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_FILL_OPTION_ID_COVER,
                            IMAGE_CREATE_FILL_OPTION_COVER_DISPLAY,
                            fillOptionId == IMAGE_CREATE_FILL_OPTION_ID_COVER,
                        ),
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_FILL_OPTION_ID_STRETCH,
                            IMAGE_CREATE_FILL_OPTION_STRETCH_DISPLAY,
                            fillOptionId == IMAGE_CREATE_FILL_OPTION_ID_STRETCH,
                        ),
                    ),
                )
                SingleOptionInput(
                    key = IMAGE_CREATE_DITHER_INPUT_KEY,
                    label = IMAGE_CREATE_DITHER_INPUT_LABEL,
                    width = 300,
                    entries = listOf(
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT,
                            IMAGE_CREATE_DITHER_OPTION_DEFAULT_DISPLAY,
                            ditherOptionId == IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT,
                        ),
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_DITHER_OPTION_ID_NONE,
                            IMAGE_CREATE_DITHER_OPTION_NONE_DISPLAY,
                            ditherOptionId == IMAGE_CREATE_DITHER_OPTION_ID_NONE,
                        ),
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_DITHER_OPTION_ID_FLOYD_STEINBERG,
                            IMAGE_CREATE_DITHER_OPTION_FLOYD_STEINBERG_DISPLAY,
                            ditherOptionId == IMAGE_CREATE_DITHER_OPTION_ID_FLOYD_STEINBERG,
                        ),
                        SingleOptionDialogInput.OptionEntry.create(
                            IMAGE_CREATE_DITHER_OPTION_ID_ORDERED_BAYER,
                            IMAGE_CREATE_DITHER_OPTION_ORDERED_BAYER_DISPLAY,
                            ditherOptionId == IMAGE_CREATE_DITHER_OPTION_ID_ORDERED_BAYER,
                        ),
                    ),
                )
            }
        )
    }
}

private fun String.toDitherMode(defaultMode: DitherMode): DitherMode = when (this) {
    IMAGE_CREATE_DITHER_OPTION_ID_NONE -> DitherMode.NONE
    IMAGE_CREATE_DITHER_OPTION_ID_ORDERED_BAYER -> DitherMode.ORDERED_BAYER
    IMAGE_CREATE_DITHER_OPTION_ID_FLOYD_STEINBERG -> DitherMode.FLOYD_STEINBERG
    IMAGE_CREATE_DITHER_OPTION_ID_DEFAULT -> defaultMode
    else -> defaultMode
}

private fun String.toRepositionMode(): RepositionMode = when (this) {
    IMAGE_CREATE_FILL_OPTION_ID_COVER -> RepositionMode.COVER
    IMAGE_CREATE_FILL_OPTION_ID_STRETCH -> RepositionMode.STRETCH
    IMAGE_CREATE_FILL_OPTION_ID_CONTAIN -> RepositionMode.CONTAIN
    else -> RepositionMode.CONTAIN
}

private fun RepositionMode.toFillOptionId(): String = when (this) {
    RepositionMode.COVER -> IMAGE_CREATE_FILL_OPTION_ID_COVER
    RepositionMode.CONTAIN -> IMAGE_CREATE_FILL_OPTION_ID_CONTAIN
    RepositionMode.STRETCH -> IMAGE_CREATE_FILL_OPTION_ID_STRETCH
}
