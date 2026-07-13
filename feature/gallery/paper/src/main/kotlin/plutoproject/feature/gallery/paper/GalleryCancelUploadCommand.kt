package plutoproject.feature.gallery.adapter.paper

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.common.upload.UploadService
import plutoproject.framework.common.util.chat.PLAYER_ONLY_COMMAND

private const val COMMAND_GALLERY_CANCEL_UPLOAD_PERMISSION = "plutoproject.gallery.command.gallery.cancel_upload"

@Suppress("UNUSED")
object GalleryCancelUploadCommand {
    private val uploadService = koin.get<UploadService>()

    @Command("gallery cancel-upload")
    @Permission(COMMAND_GALLERY_CANCEL_UPLOAD_PERMISSION)
    suspend fun CommandSender.cancelUpload() {
        val player = this as? Player
        if (player == null) {
            sendMessage(PLAYER_ONLY_COMMAND)
            return
        }

        val cancelled = uploadService.cancelUnfinishedSession(player.uniqueId)
        if (cancelled == null) {
            player.sendMessage(IMAGE_CANCEL_UPLOAD_NO_SESSION)
        }
    }
}
