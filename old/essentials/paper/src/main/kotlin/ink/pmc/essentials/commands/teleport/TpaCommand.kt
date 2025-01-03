package ink.pmc.essentials.commands.teleport

import ink.pmc.essentials.*
import ink.pmc.essentials.api.afk.AfkManager
import ink.pmc.essentials.api.teleport.TeleportDirection
import ink.pmc.essentials.api.teleport.TeleportDirection.COME
import ink.pmc.essentials.api.teleport.TeleportDirection.GO
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.screens.teleport.TeleportRequestScreen
import ink.pmc.framework.startScreen
import ink.pmc.framework.chat.DURATION
import ink.pmc.framework.chat.replace
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.java.KoinJavaComponent.getKoin

@Suppress("UNUSED")
object TpaCommand {
    @Command("tpa [player]")
    @Permission("essentials.tpa")
    fun tpa(sender: CommandSender, @Argument("player") player: Player? = null) = sender.ensurePlayer {
        handleTpa(this, player, GO)
    }

    @Command("tpahere [player]")
    @Permission("essentials.tpahere")
    fun tpahere(sender: CommandSender, @Argument("player") player: Player? = null) = sender.ensurePlayer {
        handleTpa(this, player, COME)
    }
}

private fun handleTpa(source: Player, destination: Player?, direction: TeleportDirection) {
    if (destination == null) {
        source.startScreen(TeleportRequestScreen())
        return
    }

    if (destination == source) {
        source.sendMessage(COMMAND_TPA_FAILED_SELF)
        return
    }

    if (TeleportManager.hasPendingRequest(destination)) {
        source.sendMessage(COMMAND_TPA_FAILED_TARGET_BUSY)
        return
    }

    if (direction == GO && TeleportManager.isBlacklisted(destination.world) && !source.hasPermission(
            BYPASS_WORLD_BLACKLIST
        )
    ) {
        source.sendMessage(
            COMMAND_TPA_FAILED_NOT_ALLOWED_GO
                .replace("<player>", source.name)
        )
        return
    }

    if (direction == COME && TeleportManager.isBlacklisted(source.world) && !source.hasPermission(BYPASS_WORLD_BLACKLIST)) {
        source.sendMessage(COMMAND_TPA_FAILED_NOT_ALLOWED_COME)
        return
    }

    val oldRequest = TeleportManager.getUnfinishedRequest(source)

    oldRequest?.cancel()
    TeleportManager.createRequest(source, destination, direction)

    val message = when (direction) {
        GO -> COMMAND_TPA_SUCCEED
        COME -> COMMAND_TPAHERE_SUCCEED
    }

    source.sendMessage(
        message
            .replace("<player>", destination.name)
            .replace("<expire>", DURATION(TeleportManager.defaultRequestOptions.expireAfter))
    )
    if (getKoin().get<EssentialsConfig>().afk.enabled && AfkManager.isAfk(destination)) {
        source.sendMessage(COMMAND_TPA_AFK)
    }
    oldRequest?.let {
        source.sendMessage(TELEPORT_REQUEST_AUTO_CANCEL.replace("<player>", oldRequest.destination.name))
    }
    source.playSound(TELEPORT_REQUEST_RECEIVED_SOUND)
}