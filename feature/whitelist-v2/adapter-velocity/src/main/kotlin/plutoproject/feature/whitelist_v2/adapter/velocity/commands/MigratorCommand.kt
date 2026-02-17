package plutoproject.feature.whitelist_v2.adapter.velocity.commands

import com.velocitypowered.api.command.CommandSource
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.whitelist_v2.adapter.velocity.COMMAND_WHITELIST_MIGRATE_COMPLETE
import plutoproject.feature.whitelist_v2.adapter.velocity.COMMAND_WHITELIST_MIGRATE_START
import plutoproject.feature.whitelist_v2.adapter.velocity.PERMISSION_COMMAND_WHITELIST_MIGRATE
import plutoproject.feature.whitelist_v2.adapter.velocity.WhitelistConfig
import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistOperator
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.data.convertToUuid
import java.time.Clock

@Suppress("UNUSED")
object MigratorCommand : KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val clock by inject<Clock>()
    private val oldWhitelistCollection = connectOldWhitelistCollection()
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()

    @Serializable
    private data class LegacyWhitelistModel(
        @SerialName("_id") val id: String,
        val rawName: String,
        val addedAt: Long,
    )

    private fun connectOldWhitelistCollection() = MongoConnection.getCollection<LegacyWhitelistModel>("whitelist_data")

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
            WhitelistRecordData(
                uniqueId = oldRecord.id.convertToUuid(),
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
