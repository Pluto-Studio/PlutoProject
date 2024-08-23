package ink.pmc.daily

import com.electronwill.nightconfig.core.file.FileConfig
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.mongodb.kotlin.client.coroutine.MongoCollection
import ink.pmc.daily.api.Daily
import ink.pmc.daily.listeners.DailyListener
import ink.pmc.daily.repositories.DailyHistoryRepository
import ink.pmc.daily.repositories.DailyUserRepository
import ink.pmc.provider.ProviderService
import ink.pmc.utils.PaperCm
import ink.pmc.utils.command.registerCommands
import ink.pmc.utils.inject.startKoinIfNotPresent
import ink.pmc.utils.storage.saveResourceIfNotExisted
import net.milkbowl.vault.economy.Economy
import org.incendo.cloud.execution.ExecutionCoordinator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import java.io.File

internal const val COMMANDS_PACKAGE = "ink.pmc.daily.commands"
internal const val COLLECTION_PREFIX = "daily_"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return ProviderService.defaultMongoDatabase.getCollection("${COLLECTION_PREFIX}$name")
}

private val bukkitModule = module {
    single<DailyConfig> { DailyConfig(fileConfig) }
    single<DailyUserRepository> { DailyUserRepository(getCollection("${COLLECTION_PREFIX}users")) }
    single<DailyHistoryRepository> { DailyHistoryRepository(getCollection("${COLLECTION_PREFIX}history")) }
    single<Daily> { DailyImpl() }
}

internal lateinit var plugin: PaperPlugin
internal lateinit var fileConfig: FileConfig
internal lateinit var commandManager: PaperCm
internal lateinit var economy: Economy

@Suppress("UNUSED")
class PaperPlugin : SuspendingJavaPlugin(), KoinComponent {

    private val daily by inject<Daily>()

    override fun onEnable() {
        plugin = this
        fileConfig = saveResourceIfNotExisted("config.conf").loadConfig()

        startKoinIfNotPresent {
            modules(bukkitModule)
        }

        commandManager = PaperCm.builder()
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
            .buildOnEnable(this)
            .also { it.registerCommands(COMMANDS_PACKAGE) }

        server.pluginManager.registerEvents(DailyListener, this)
        server.servicesManager.getRegistration(Economy::class.java)?.provider?.also { economy = it }

        if (::economy.isInitialized) {
            daily.registerPostCallback("coin_claim", coinClaim)
        }
    }

    private fun File.loadConfig(): FileConfig {
        return FileConfig.builder(this)
            .async()
            .autoreload()
            .build()
            .also { it.load() }
    }

    internal fun reload() {
        fileConfig.load()
    }

}