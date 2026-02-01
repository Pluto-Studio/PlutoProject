package plutoproject.feature.velocity.whitelist_v2.commands

import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.whitelist_v2.model.WhitelistOperatorModel
import plutoproject.feature.common.whitelist_v2.model.WhitelistOperatorModelType
import plutoproject.feature.common.whitelist_v2.model.WhitelistRecordModel
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.feature.velocity.whitelist.WhitelistModel
import plutoproject.feature.velocity.whitelist.WhitelistRepository
import plutoproject.feature.velocity.whitelist_v2.COMMAND_WHITELIST_MIGRATE_COMPLETE
import plutoproject.feature.velocity.whitelist_v2.COMMAND_WHITELIST_MIGRATE_START
import plutoproject.feature.velocity.whitelist_v2.PERMISSION_COMMAND_WHITELIST_MIGRATE
import plutoproject.feature.velocity.whitelist_v2.WhitelistConfig
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.data.convertToUuid

@Suppress("UNUSED")
object MigratorCommand : KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val oldWhitelistRepository = connectOldWhitelistRepo()
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()

    private fun connectOldWhitelistRepo(): WhitelistRepository {
        val collection = MongoConnection.getCollection<WhitelistModel>("whitelist_data")
        return WhitelistRepository(collection)
    }

    @Command("whitelist migrate")
    @Permission(PERMISSION_COMMAND_WHITELIST_MIGRATE)
    suspend fun CommandSource.migrate() {
        if (!config.enableMigrator) return
        sendMessage(COMMAND_WHITELIST_MIGRATE_START)

        val oldRecords = oldWhitelistRepository.findAll()
        val modelsToMigrate = mutableListOf<WhitelistRecordModel>()

        oldRecords.forEach { oldRecord ->
            val model = WhitelistRecordModel(
                uniqueId = oldRecord.id.convertToUuid(),
                username = oldRecord.rawName,
                granter = WhitelistOperatorModel(type = WhitelistOperatorModelType.CONSOLE),
                joinedAsVisitorBefore = false,
                isMigrated = true
            )
            modelsToMigrate.add(model)
        }

        whitelistRecordRepository.insertAll(modelsToMigrate)
        sendMessage(COMMAND_WHITELIST_MIGRATE_COMPLETE.replace("<count>", modelsToMigrate.size.toString()))
    }
}
