package plutoproject.feature.lecternprotection.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.block.Lectern
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.foundation.common.serialization.uuidOrNull
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.paper.command.ensurePlayer
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

const val LECTERN_PROTECTION_BYPASS_PERMISSION = "plutoproject.lectern_protection.bypass"

private val protectKey = NamespacedKey("essentials", "lectern_protect")
private val protectorKey = NamespacedKey("essentials", "lectern_protector")

internal val Lectern.protect: Boolean
    get() = persistentDataContainer.getOrDefault(protectKey, PersistentDataType.BOOLEAN, false)

internal val Lectern.protector: OfflinePlayer?
    get() {
        val uuid = persistentDataContainer.get(protectorKey, PersistentDataType.STRING)
            ?.uuidOrNull() ?: return null
        return Bukkit.getOfflinePlayer(uuid)
    }

internal val Lectern.isProtected: Boolean
    get() = protect && protector != null

internal val Lectern.protectorName: String
    get() = protector?.name ?: LECTERN_PROTECTION_UNKNOWN_PLAYER

private fun Lectern.setProtect(value: Boolean, player: Player) {
    persistentDataContainer.set(protectKey, PersistentDataType.BOOLEAN, value)
    if (value) {
        persistentDataContainer.set(protectorKey, PersistentDataType.STRING, player.uniqueId.toString())
        update()
        return
    }
    persistentDataContainer.remove(protectorKey)
    update()
}

@Suppress("UNUSED")
object LecternCommand {
    private val serverContext = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

    @Command("lectern")
    @Permission("plutoproject.lectern_protection.command.lectern")
    suspend fun CommandSender.lectern() = ensurePlayer {
        withContext(serverContext) {
            val range = getAttribute(Attribute.BLOCK_INTERACTION_RANGE)!!.value
            val block = getTargetBlockExact(range.toInt())?.state
            val player = this@ensurePlayer

            if (block == null || block !is Lectern) {
                sendMessage(COMMAND_LECTERN_FAILED_NO_LECTERN)
                return@withContext
            }
            if (block.isProtected
                && block.protector != player
                && !player.hasPermission(LECTERN_PROTECTION_BYPASS_PERMISSION)
            ) {
                sendMessage(LECTERN_PROTECTED_ON_ACTION.replace("<player>", block.protectorName))
                return@withContext
            }
            if (!block.protect) {
                block.setProtect(true, player)
                sendMessage(COMMAND_LECTERN_PROTECTION_ON_SUCCEED)
                return@withContext
            }

            block.setProtect(false, player)
            sendMessage(COMMAND_LECTERN_PROTECTION_OFF_SUCCEED)
            return@withContext
        }
    }
}
