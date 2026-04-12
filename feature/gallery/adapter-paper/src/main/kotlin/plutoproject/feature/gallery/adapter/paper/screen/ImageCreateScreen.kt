package plutoproject.feature.gallery.adapter.paper.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.event.ClickCallback
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_CANCEL
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_DESCRIPTION
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_FAILED_EMPTY_NAME
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_FAILED_TOO_LONG
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_HEIGHT_INPUT_LABEL
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_NAME_INPUT_LABEL
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_FAILED_TOO_MANY_IMAGES
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_FAILED_TOO_MANY_MAP_BLOCKS
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_FAILED_UNFINISHED_UPLOAD
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_SUBMIT
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_TITLE
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_WIDTH_INPUT_LABEL
import plutoproject.feature.gallery.adapter.paper.ImageCreateSubmissionResult
import plutoproject.feature.gallery.adapter.paper.submitImageCreate
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_MAX_MAP_BLOCKS
import plutoproject.feature.gallery.adapter.paper.IMAGE_PLACEHOLDER_MAX_NAME_LENGTH
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.dialog.Dialog
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.NumberRangeInput
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.TextInput
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.entity.clearDialog

private const val IMAGE_CREATE_NAME_INPUT_KEY = "name"
private const val IMAGE_CREATE_WIDTH_INPUT_KEY = "width"
private const val IMAGE_CREATE_HEIGHT_INPUT_KEY = "height"

class ImageCreateScreen : InteractiveScreen() {
    @Composable
    @Suppress("UnstableApiUsage")
    override fun Content() {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        val imageConfig = remember { koin.get<GalleryConfig>().image }

        var name by rememberSaveable { mutableStateOf("") }
        var width by rememberSaveable { mutableStateOf(1f) }
        var height by rememberSaveable { mutableStateOf(1f) }

        val cancelCallback = DialogAction.customClick(
            { _, _ -> navigator.pop() },
            ClickCallback.Options.builder().build()
        )
        val submitCallback = DialogAction.customClick({ view, _ ->
            val submittedName = view.getText(IMAGE_CREATE_NAME_INPUT_KEY) ?: ""
            val submittedWidth = (view.getFloat(IMAGE_CREATE_WIDTH_INPUT_KEY) ?: 1f).toInt()
            val submittedHeight = (view.getFloat(IMAGE_CREATE_HEIGHT_INPUT_KEY) ?: 1f).toInt()
            val mapCount = runCatching { Math.multiplyExact(submittedWidth, submittedHeight) }.getOrNull()

            name = submittedName
            width = submittedWidth.toFloat()
            height = submittedHeight.toFloat()

            if (submittedName.isBlank()) {
                player.playSound(UI_FAILED_SOUND)
                navigator.push(ImageCreateFeedbackScreen(IMAGE_CREATE_FAILED_EMPTY_NAME))
                return@customClick
            }

            if (submittedName.length > imageConfig.maxNameLength) {
                player.playSound(UI_FAILED_SOUND)
                navigator.push(
                    ImageCreateFeedbackScreen(
                        IMAGE_CREATE_FAILED_TOO_LONG.replace(IMAGE_PLACEHOLDER_MAX_NAME_LENGTH, imageConfig.maxNameLength)
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
                when (submitImageCreate(player, submittedName, submittedWidth, submittedHeight)) {
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
                PlainMessageBody(IMAGE_CREATE_DESCRIPTION)
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
            }
        )
    }
}
