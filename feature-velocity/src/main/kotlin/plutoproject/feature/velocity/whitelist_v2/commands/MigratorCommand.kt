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
import plutoproject.feature.velocity.whitelist.WhitelistRepository
import plutoproject.feature.velocity.whitelist_v2.COMMAND_WHITELIST_MIGRATE_COMPLETE
import plutoproject.feature.velocity.whitelist_v2.COMMAND_WHITELIST_MIGRATE_START
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.data.convertToUuid

@Suppress("UNUSED")
object MigratorCommand : KoinComponent {
    private val oldWhitelistRepository by inject<WhitelistRepository>()
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()

    @Command("whitelist migrate")
    @Permission("whitelist.command.migrate")
    suspend fun CommandSource.migrate() {
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
