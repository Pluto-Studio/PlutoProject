package plutoproject.feature.paper.itemFrameProtection

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
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.data.convertToUuidOrNull
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.coroutine.coroutineContext

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
            persistentDataContainer.get(protectorKey, PersistentDataType.STRING)?.convertToUuidOrNull() ?: return null
        return Bukkit.getOfflinePlayer(uuid)
    }

internal val ItemFrame.isProtected: Boolean
    get() = protect && protector != null

internal val ItemFrame.protectorName: String
    get() = protector?.name ?: ITEMFRAME_PROTECTION_UNKNOWN_PLAYER

private fun ItemFrame.setProtect(value: Boolean, player: Player) {
    persistentDataContainer.set(protectKey, PersistentDataType.BOOLEAN, value)
    if (value) {
        persistentDataContainer.set(protectorKey, PersistentDataType.STRING, player.uniqueId.toString())
        return
    }
    persistentDataContainer.remove(protectorKey)
}

@Suppress("UNUSED")
object ItemFrameCommand {
    @Command("itemframe|if invisible")
    @Permission("plutoproject.itemframe_protection.command.itemframe.invisible")
    suspend fun CommandSender.invisible() = ensurePlayer {
        withContext(coroutineContext) {
            handleOperation(Operation.INVISIBLE)
        }
    }

    @Command("itemframe|if protect")
    @Permission("plutoproject.itemframe_protection.command.itemframe.protect")
    suspend fun CommandSender.protect() = ensurePlayer {
        withContext(coroutineContext) {
            handleOperation(Operation.PROTECT)
        }
    }
}

private fun Player.handleOperation(operation: Operation) {
    val range = getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value
    val entity = getTargetEntity(range.toInt())

    if (entity == null || entity !is ItemFrame) {
        sendMessage(COMMAND_ITEMFRAME_FAILED_NO_ITEMFRAME)
        return
    }

    val player = this

    fun ItemFrame.handleInvisible() {
        if (isProtected && protector != player && !player.hasPermission(ITEMFRAME_PROTECTION_BYPASS_PERMISSION)) {
            player.sendMessage(ITEMFRAME_PROTECTED.replace("<player>", protectorName))
            return
        }
        if (!inv) {
            inv = true
            player.sendMessage(COMMAND_ITEMFRAME_TOGGLE_ON_INVISBLE)
            return
        }
        inv = false
        player.sendMessage(COMMAND_ITEMFRAME_TOGGLE_OFF_INVISBLE)
    }

    fun ItemFrame.handleProtect() {
        if (isProtected && protector != player && !player.hasPermission(ITEMFRAME_PROTECTION_BYPASS_PERMISSION)) {
            player.sendMessage(ITEMFRAME_PROTECTED.replace("<player>", protectorName))
            return
        }
        if (!protect) {
            setProtect(true, player)
            player.sendMessage(COMMAND_ITEMFRAME_TOGGLE_ON_PROTECTION)
            return
        }
        setProtect(false, player)
        player.sendMessage(COMMAND_ITEMFRAME_TOGGLE_OFF_PROTECTION)
        return
    }

    when (operation) {
        Operation.INVISIBLE -> entity.handleInvisible()
        Operation.PROTECT -> entity.handleProtect()
    }
}
