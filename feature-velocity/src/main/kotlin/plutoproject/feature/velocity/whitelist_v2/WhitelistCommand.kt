package plutoproject.feature.velocity.whitelist_v2

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.WhitelistOperator
import plutoproject.feature.common.api.whitelist_v2.WhitelistRevokeReason
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.framework.common.api.profile.fetcher.FetchedData
import plutoproject.framework.common.api.profile.fetcher.MojangProfileFetcher
import plutoproject.framework.common.util.chat.component.replace
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object WhitelistCommand : KoinComponent {
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()

    private suspend fun fetchProfileWithTimeout(username: String): FetchedData? {
        return try {
            withTimeout(10.seconds) {
                MojangProfileFetcher.fetchByName(username)
            }
        } catch (e: TimeoutCancellationException) {
            return null
        }
    }

    @Command("whitelist grant <name>")
    @Permission("whitelist.command.grant")
    suspend fun CommandSource.grant(@Argument("name") name: String) {
        sendMessage(COMMAND_WHITELIST_ADD_FETCHING)
        val operator = if (this is Player) WhitelistOperator.Administrator(uniqueId) else WhitelistOperator.Console
        val profile = fetchProfileWithTimeout(name)

        if (profile == null) {
            sendMessage(COMMAND_WHITELIST_ADD_NOT_FOUND.replace("<name>", name))
            return
        }
        if (Whitelist.isWhitelisted(profile.uuid)) {
            sendMessage(COMMAND_WHITELIST_ADD_ALREADY_EXISTS.replace("<name>", profile.name))
            return
        }

        val success = Whitelist.grantWhitelist(
            uniqueId = profile.uuid,
            username = profile.name,
            operator = operator
        )

        if (success) {
            sendMessage(COMMAND_WHITELIST_ADD_SUCCEED.replace("<name>", profile.name))
        } else {
            sendMessage(COMMAND_WHITELIST_ADD_ALREADY_EXISTS.replace("<name>", profile.name))
        }
    }

    @Command("whitelist revoke <name> <reason>")
    @Permission("whitelist.command.revoke")
    suspend fun CommandSource.remove(
        @Argument("name") name: String,
        @Argument("reason") reason: WhitelistRevokeReason
    ) {
        sendMessage(COMMAND_WHITELIST_ADD_FETCHING)
        val operator = if (this is Player) WhitelistOperator.Administrator(uniqueId) else WhitelistOperator.Console
        val profile = fetchProfileWithTimeout(name)

        if (profile == null) {
            sendMessage(COMMAND_WHITELIST_ADD_NOT_FOUND.replace("<name>", name))
            return
        }
        if (!Whitelist.isWhitelisted(profile.uuid)) {
            sendMessage(COMMAND_WHITELIST_REMOVE_NOT_FOUND.replace("<name>", profile.name))
            return
        }

        val success = Whitelist.revokeWhitelist(
            uniqueId = profile.uuid,
            operator = operator,
            reason = WhitelistRevokeReason.OTHER
        )

        if (success) {
            sendMessage(COMMAND_WHITELIST_REMOVE_SUCCEED.replace("<name>", profile.name))
        } else {
            sendMessage(COMMAND_WHITELIST_REMOVE_NOT_FOUND.replace("<name>", profile.name))
        }
    }

    @Command("whitelist lookup <name>")
    @Permission("whitelist.command.lookup")
    suspend fun CommandSource.lookup(@Argument("name") name: String) {
        sendMessage(COMMAND_WHITELIST_ADD_FETCHING)
        val profile = fetchProfileWithTimeout(name)

        if (profile == null) {
            sendMessage(COMMAND_WHITELIST_ADD_NOT_FOUND.replace("<name>", name))
            return
        }
        if (Whitelist.isWhitelisted(profile.uuid)) {
            sendMessage(COMMAND_WHITELIST_LOOKUP_WHITELISTED.replace("<name>", profile.name))
            return
        }

        sendMessage(COMMAND_WHITELIST_LOOKUP_NOT_FOUND.replace("<name>", profile.name))
    }

    @Command("whitelist statistic")
    @Permission("whitelist.command")
    suspend fun CommandSource.statistic() {
        val allRecords = whitelistRecordRepository.findAll()
        val count = allRecords.count { !it.isRevoked }
        sendMessage(COMMAND_WHITELIST_STATISTIC.replace("<count>", count.toString()))
    }
}
