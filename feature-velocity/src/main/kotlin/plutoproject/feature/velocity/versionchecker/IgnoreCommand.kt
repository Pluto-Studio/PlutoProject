package plutoproject.feature.velocity.versionchecker

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.Command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.IntTypeAdapter
import plutoproject.framework.velocity.util.command.ensurePlayer

@Suppress("UNUSED")
object IgnoreCommand : KoinComponent {
    private val config by inject<VersionCheckerConfig>()

    @Command("ignore-version-warning")
    suspend fun ignoreVersionWarning(sender: CommandSource) = ensurePlayer(sender) {
        val container = DatabasePersist.getContainer(uniqueId)
        val ignoredProtocol = container.get(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter)

        if (protocolVersion.protocol in config.supportedProtocolRange) {
            sendMessage(COMMAND_IGNORE_FAILED_NOT_ON_UNSUPPORTED_VERSION)
            return@ensurePlayer
        }

        if (ignoredProtocol != null && ignoredProtocol == config.minimumSupportedProtocol) {
            container.remove(IGNORED_PROTOCOL_PERSIST_KEY)
            sendMessage(COMMAND_IGNORE_DISABLED)
            container.save()
            return@ensurePlayer
        }

        container.set(IGNORED_PROTOCOL_PERSIST_KEY, IntTypeAdapter, config.minimumSupportedProtocol)
        container.save()
        sendMessage(COMMAND_IGNORE_ENABLED)
    }
}
