package plutoproject.feature.whitelist.velocity.commands

import com.velocitypowered.api.command.CommandSource
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.feature.whitelist.velocity.COMMAND_WHITELIST_MIGRATE_COMPLETE
import plutoproject.feature.whitelist.velocity.COMMAND_WHITELIST_MIGRATE_START
import plutoproject.feature.whitelist.velocity.PERMISSION_COMMAND_WHITELIST_MIGRATE
import plutoproject.feature.whitelist.velocity.WhitelistConfig
import plutoproject.feature.whitelist.core.WhitelistRecord
import plutoproject.feature.whitelist.core.WhitelistOperator
import plutoproject.feature.whitelist.core.WhitelistRecordRepository
import plutoproject.foundation.common.text.replace
import java.time.Clock
import java.util.UUID
import plutoproject.kernel.api.koinGet

@Suppress("UNUSED")
object MigratorCommand {
    private val config = koinGet<WhitelistConfig>()
    private val clock = koinGet<Clock>()
    private val whitelistRecordRepository = koinGet<WhitelistRecordRepository>()
    private val oldWhitelistCollection = koinGet<MongoConnection>().getCollection<LegacyWhitelistModel>("whitelist_data")

    @Serializable
    private data class LegacyWhitelistModel(
        @SerialName("_id") val id: String,
        val rawName: String,
        val addedAt: Long,
    )

    private suspend fun loadLegacyWhitelist(): List<LegacyWhitelistModel> {
        return oldWhitelistCollection.find().toList()
    }

    @Command("whitelist migrate")
    @Permission(PERMISSION_COMMAND_WHITELIST_MIGRATE)
    suspend fun CommandSource.migrate() {
        if (!config.enableMigrator) return
        sendMessage(COMMAND_WHITELIST_MIGRATE_START)

        val oldRecords = loadLegacyWhitelist()
        val recordsToMigrate = oldRecords.map { oldRecord ->
            WhitelistRecord(
                uniqueId = UUID.fromString(oldRecord.id),
                username = oldRecord.rawName,
                granter = WhitelistOperator.Console,
                createdAt = clock.instant(),
                joinedAsVisitorBefore = false,
                isMigrated = true,
                isRevoked = false,
                revoker = null,
                revokeReason = null,
                revokeAt = null,
            )
        }

        whitelistRecordRepository.insertAll(recordsToMigrate)
        sendMessage(COMMAND_WHITELIST_MIGRATE_COMPLETE.replace("<count>", recordsToMigrate.size.toString()))
    }
}
