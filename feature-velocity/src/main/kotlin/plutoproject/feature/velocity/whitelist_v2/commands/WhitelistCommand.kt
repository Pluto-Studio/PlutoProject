package plutoproject.feature.velocity.whitelist_v2.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.raw
import ink.pmc.advkt.send
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.text.Component
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.WhitelistOperator
import plutoproject.feature.common.api.whitelist_v2.WhitelistRevokeReason
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.feature.velocity.whitelist_v2.*
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.api.profile.fetcher.FetchedData
import plutoproject.framework.common.api.profile.fetcher.MojangProfileFetcher
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.time.format
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object WhitelistCommand : KoinComponent {
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()

    // Pair 里的第一个元素为 true 即为超时导致的未获取
    private suspend fun fetchProfileWithTimeout(username: String): Pair<Boolean, FetchedData?> {
        return try {
            val profile = withTimeout(10.seconds) {
                MojangProfileFetcher.fetchByName(username)
            }
            false to profile
        } catch (e: TimeoutCancellationException) {
            true to null
        }
    }

    @Command("whitelist grant <name>")
    @Permission("whitelist.command.grant")
    suspend fun CommandSource.grant(@Argument("name") name: String) {
        sendMessage(COMMAND_WHITELIST_ADD_FETCHING)
        val operator = if (this is Player) WhitelistOperator.Administrator(uniqueId) else WhitelistOperator.Console
        val profile = fetchProfileWithTimeout(name)

        if (profile.second == null) {
            if (profile.first) {
                sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_TIMEOUT.replace("<name>", name))
                return
            }
            sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_NOT_FOUND.replace("<name>", name))
            return
        }

        val fetchedProfile = profile.second!!

        if (Whitelist.isWhitelisted(fetchedProfile.uuid)) {
            sendMessage(COMMAND_WHITELIST_ADD_ALREADY_EXISTS.replace("<name>", fetchedProfile.name))
            return
        }

        val success = Whitelist.grantWhitelist(
            uniqueId = fetchedProfile.uuid,
            username = fetchedProfile.name,
            operator = operator
        )

        if (success) {
            sendMessage(COMMAND_WHITELIST_ADD_SUCCEED.replace("<name>", fetchedProfile.name))
        } else {
            sendMessage(COMMAND_WHITELIST_ADD_ALREADY_EXISTS.replace("<name>", fetchedProfile.name))
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

        if (profile.second == null) {
            if (profile.first) {
                sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_TIMEOUT.replace("<name>", name))
                return
            }
            sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_NOT_FOUND.replace("<name>", name))
            return
        }

        val fetchedProfile = profile.second!!

        if (!Whitelist.isWhitelisted(fetchedProfile.uuid)) {
            sendMessage(COMMAND_WHITELIST_REMOVE_NOT_FOUND.replace("<name>", fetchedProfile.name))
            return
        }

        val success = Whitelist.revokeWhitelist(
            uniqueId = fetchedProfile.uuid,
            operator = operator,
            reason = WhitelistRevokeReason.OTHER
        )

        if (success) {
            sendMessage(COMMAND_WHITELIST_REMOVE_SUCCEED.replace("<name>", fetchedProfile.name))
        } else {
            sendMessage(COMMAND_WHITELIST_REMOVE_NOT_FOUND.replace("<name>", fetchedProfile.name))
        }
    }

    @Command("whitelist lookup <name>")
    @Permission("whitelist.command.lookup")
    suspend fun CommandSource.lookup(@Argument("name") name: String) {
        sendMessage(COMMAND_WHITELIST_ADD_FETCHING)
        val profile = fetchProfileWithTimeout(name)

        if (profile.second == null) {
            if (profile.first) {
                sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_TIMEOUT.replace("<name>", name))
                return
            }
            sendMessage(COMMAND_WHITELIST_PROFILE_FETCH_NOT_FOUND.replace("<name>", name))
            return
        }

        val fetchedProfile = profile.second!!
        val record = Whitelist.lookupWhitelistRecord(fetchedProfile.uuid)

        if (record == null) {
            sendMessage(COMMAND_WHITELIST_LOOKUP_NO_RECORD.replace("<name>", fetchedProfile.name))
            return
        }

        val formattedGranter = formatOperator(record.granter)
        val formatterRevoker = record.revoker?.let { formatOperator(it) }

        send {
            raw(COMMAND_WHITELIST_LOOKUP_HEADER.replace("<name>", record.username))
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_UUID.replace("<uuid>", record.uniqueId.toString()))
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_USERNAME.replace("<username>", record.username))
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_GRANTER.replace("<granter>", formattedGranter))
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_CREATED_AT.replace("<created_at>", record.createdAt.format()))
            newline()
            raw(
                COMMAND_WHITELIST_LOOKUP_VISITOR_BEFORE.replace(
                    "<status>",
                    if (record.joinedAsVisitorBefore) "是" else "否"
                )
            )
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_MIGRATED.replace("<status>", if (record.isMigrated) "是" else "否"))
            newline()
            raw(COMMAND_WHITELIST_LOOKUP_REVOKED.replace("<status>", if (record.isRevoked) "已撤销" else "有效"))
            if (record.isRevoked) {
                newline()
                raw(COMMAND_WHITELIST_LOOKUP_REVOKER.replace("<revoker>", formatterRevoker!!))
                newline()
                raw(
                    COMMAND_WHITELIST_LOOKUP_REVOKE_REASON.replace(
                        "<reason>",
                        formatRevokeReason(record.revokeReason!!)
                    )
                )
                newline()
                raw(COMMAND_WHITELIST_LOOKUP_REVOKE_TIME.replace("<time>", record.revokeAt!!.format()))
            }
        }
    }

    private suspend fun formatOperator(operator: WhitelistOperator): Component {
        return when (operator) {
            is WhitelistOperator.Console -> COMMAND_WHITELIST_OPERATOR_CONSOLE
            is WhitelistOperator.Administrator -> {
                val profile = ProfileLookup.lookupByUuid(operator.uniqueId, requestApi = false)
                val name = profile?.name ?: operator.uniqueId.toString()
                COMMAND_WHITELIST_OPERATOR_ADMIN.replace("<name>", name)
            }
        }
    }

    private fun formatRevokeReason(reason: WhitelistRevokeReason): Component {
        return when (reason) {
            WhitelistRevokeReason.VIOLATION -> COMMAND_WHITELIST_REVOKE_REASON_VIOLATION
            WhitelistRevokeReason.REQUESTED -> COMMAND_WHITELIST_REVOKE_REASON_REQUESTED
            WhitelistRevokeReason.OTHER -> COMMAND_WHITELIST_REVOKE_REASON_OTHER
        }
    }

    @Command("whitelist statistic")
    @Permission("whitelist.command")
    suspend fun CommandSource.statistic() {
        val allRecords = whitelistRecordRepository.findAll()
        val count = allRecords.count { !it.isRevoked }
        sendMessage(COMMAND_WHITELIST_STATISTIC.replace("<count>", count.toString()))
    }
}
