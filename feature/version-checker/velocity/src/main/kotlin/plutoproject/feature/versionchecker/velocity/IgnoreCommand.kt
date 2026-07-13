package plutoproject.feature.versionchecker.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import org.incendo.cloud.annotations.Command
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.databasepersist.api.adapters.IntTypeAdapter

internal class IgnoreCommand(
    private val config: VersionCheckerConfig,
    private val databasePersist: DatabasePersist,
) {
    @Command("ignore-version-warning")
    suspend fun ignoreVersionWarning(sender: CommandSource) {
        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(COMMAND_PLAYERS_ONLY)
            return
        }

        val container = databasePersist.getContainer(player.uniqueId)
        val ignoredProtocol = container.get(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter)
        if (player.protocolVersion.protocol in config.supportedProtocolRange) {
            player.sendMessage(COMMAND_IGNORE_FAILED_NOT_ON_UNSUPPORTED_VERSION)
            return
        }

        if (ignoredProtocol == config.minimumSupportedProtocol) {
            container.remove(IGNORED_PROTOCOL_PERSIST_KEY)
            player.sendMessage(COMMAND_IGNORE_DISABLED)
        } else {
            container.set(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter, config.minimumSupportedProtocol)
            player.sendMessage(COMMAND_IGNORE_ENABLED)
        }
        container.save()
    }
}
