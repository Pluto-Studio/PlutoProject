package plutoproject.feature.itemframeprotection.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.gallery.paper.imageItemFrameData
import plutoproject.foundation.common.serialization.uuidOrNull
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

const val ITEMFRAME_PROTECTION_BYPASS_PERMISSION = "plutoproject.itemframe_protection.bypass"

private val invKey = NamespacedKey("essentials", "itemframe_invsible")
private val protectKey = NamespacedKey("essentials", "itemframe_protect")
private val protectorKey = NamespacedKey("essentials", "itemframe_protector")

private enum class Operation {
    INVISIBLE, PROTECT
}

internal var ItemFrame.inv: Boolean
    get() = persistentDataContainer.getOrDefault(invKey, PersistentDataType.BOOLEAN, false)
    set(value) {
        persistentDataContainer.set(invKey, PersistentDataType.BOOLEAN, value)
        isVisible = !value
    }

internal val ItemFrame.protect: Boolean
    get() = persistentDataContainer.getOrDefault(protectKey, PersistentDataType.BOOLEAN, false)

internal val ItemFrame.protector: OfflinePlayer?
    get() {
        val uuid =
            persistentDataContainer.get(protectorKey, PersistentDataType.STRING)?.uuidOrNull() ?: return null
        return Bukkit.getOfflinePlayer(uuid)
    }

internal val ItemFrame.isProtected: Boolean
    get() = protect && protector != null

internal val ItemFrame.protectorName: String
    get() = protector?.name ?: ITEMFRAME_PROTECTION_UNKNOWN_PLAYER

internal fun ItemFrame.setProtect(value: Boolean, player: Player) {
    persistentDataContainer.set(protectKey, PersistentDataType.BOOLEAN, value)
    if (value) {
        persistentDataContainer.set(protectorKey, PersistentDataType.STRING, player.uniqueId.toString())
        return
    }
    persistentDataContainer.remove(protectorKey)
}

internal fun ItemFrame.clearProtect() {
    persistentDataContainer.remove(protectKey)
    persistentDataContainer.remove(protectorKey)
}

@Suppress("UNUSED")
object ItemFrameCommand {
    private val serverContext = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

    @Command("itemframe|if invisible")
    @Permission("plutoproject.itemframe_protection.command.itemframe.invisible")
    suspend fun CommandSender.invisible() = ensurePlayer {
        withContext(serverContext) {
            handleOperation(Operation.INVISIBLE)
        }
    }

    @Command("itemframe|if protect")
    @Permission("plutoproject.itemframe_protection.command.itemframe.protect")
    suspend fun CommandSender.protect() = ensurePlayer {
        withContext(serverContext) {
            handleOperation(Operation.PROTECT)
        }
    }
}

private suspend fun Player.handleOperation(operation: Operation) {
    val range = getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value
    val entity = getTargetEntity(range.toInt())

    if (entity == null || entity !is ItemFrame) {
        sendMessage(COMMAND_ITEMFRAME_FAILED_NO_ITEMFRAME)
        return
    }

    val itemFrames = resolveOperationFrames(entity)
    val isGalleryOperation = GalleryIntegration.isGalleryItemFrame(entity)
    val protectedFrame = itemFrames.firstOrNull { !it.canOperate(this) }
    if (protectedFrame != null) {
        sendMessage(ITEMFRAME_PROTECTED.replace("<player>", protectedFrame.protectorName))
        return
    }

    when (operation) {
        Operation.INVISIBLE -> handleInvisible(itemFrames, entity, isGalleryOperation)
        Operation.PROTECT -> handleProtect(itemFrames, entity, isGalleryOperation)
    }
}

private fun ItemFrame.canOperate(player: Player): Boolean {
    return !isProtected || protector == player || player.hasPermission(ITEMFRAME_PROTECTION_BYPASS_PERMISSION)
}

private fun Player.handleInvisible(itemFrames: List<ItemFrame>, target: ItemFrame, isGalleryOperation: Boolean) {
    if (!target.inv) {
        itemFrames.forEach { it.inv = true }
        sendMessage(if (isGalleryOperation) COMMAND_ITEMFRAME_TOGGLE_ON_GALLERY_INVISBLE else COMMAND_ITEMFRAME_TOGGLE_ON_INVISBLE)
        return
    }
    itemFrames.forEach { it.inv = false }
    sendMessage(if (isGalleryOperation) COMMAND_ITEMFRAME_TOGGLE_OFF_GALLERY_INVISBLE else COMMAND_ITEMFRAME_TOGGLE_OFF_INVISBLE)
}

private fun Player.handleProtect(itemFrames: List<ItemFrame>, target: ItemFrame, isGalleryOperation: Boolean) {
    if (!target.protect) {
        itemFrames.forEach { it.setProtect(true, this) }
        sendMessage(if (isGalleryOperation) COMMAND_ITEMFRAME_TOGGLE_ON_GALLERY_PROTECTION else COMMAND_ITEMFRAME_TOGGLE_ON_PROTECTION)
        return
    }
    itemFrames.forEach { it.setProtect(false, this) }
    sendMessage(if (isGalleryOperation) COMMAND_ITEMFRAME_TOGGLE_OFF_GALLERY_PROTECTION else COMMAND_ITEMFRAME_TOGGLE_OFF_PROTECTION)
}

internal suspend fun resolveOperationFrames(itemFrame: ItemFrame): List<ItemFrame> {
    if (!GalleryIntegration.isAvailable) return listOf(itemFrame)

    val frameData = itemFrame.imageItemFrameData() ?: return listOf(itemFrame)
    val originFrame = itemFrame.world.getEntity(frameData.originItemFrame) as? ItemFrame
    if (originFrame != null) {
        val frames = collectGalleryFrames(originFrame)
        if (frames.isNotEmpty()) return frames
    }

    val itemFrameIds =
        GalleryIntegration.getDisplayItemFrameIds(frameData.displayInstanceId) ?: return listOf(itemFrame)
    return itemFrameIds
        .mapNotNull { itemFrame.world.getEntity(it) as? ItemFrame }
        .ifEmpty { listOf(itemFrame) }
}

private fun collectGalleryFrames(originFrame: ItemFrame): List<ItemFrame> {
    val result = mutableListOf<ItemFrame>()
    var currentFrame: ItemFrame? = originFrame

    while (currentFrame != null) {
        result += currentFrame
        val frameData = currentFrame.imageItemFrameData()
        currentFrame = frameData?.nextItemFrame?.let { originFrame.world.getEntity(it) as? ItemFrame }
    }

    return result
}
