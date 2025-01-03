package ink.pmc.daily

import com.electronwill.nightconfig.core.file.FileConfig
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.sksamuel.hoplite.PropertySource
import ink.pmc.daily.api.Daily
import ink.pmc.daily.button.DAILY_BUTTON_DESCRIPTOR
import ink.pmc.daily.button.Daily
import ink.pmc.daily.commands.CheckInCommand
import ink.pmc.daily.commands.DailyCommand
import ink.pmc.daily.listeners.DailyListener
import ink.pmc.daily.repositories.DailyHistoryRepository
import ink.pmc.daily.repositories.DailyUserRepository
import ink.pmc.framework.provider.Provider
import ink.pmc.framework.command.annotationParser
import ink.pmc.framework.command.commandManager
import ink.pmc.framework.config.preconfiguredConfigLoaderBuilder
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.storage.saveResourceIfNotExisted
import ink.pmc.menu.api.MenuManager
import ink.pmc.menu.api.isMenuAvailable
import net.milkbowl.vault.economy.Economy
import org.koin.core.component.KoinComponent
import org.koin.dsl.module
import java.io.File

internal const val COLLECTION_PREFIX = "daily_"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return Provider.defaultMongoDatabase.getCollection("${COLLECTION_PREFIX}$name")
}

internal lateinit var plugin: PaperPlugin
internal lateinit var fileConfig: FileConfig
internal lateinit var economy: Economy

@Suppress("UNUSED")
class PaperPlugin : SuspendingJavaPlugin(), KoinComponent {
    private val bukkitModule = module {
        single<DailyConfig> {
            preconfiguredConfigLoaderBuilder()
                .addPropertySource(PropertySource.file(saveResourceIfNotExisted("config.conf")))
                .build()
                .loadConfigOrThrow()
        }
        single<DailyUserRepository> { DailyUserRepository(getCollection("users")) }
        single<DailyHistoryRepository> { DailyHistoryRepository(getCollection("history")) }
        single<Daily> { DailyImpl() }
    }

    override fun onEnable() {
        plugin = this
        fileConfig = saveResourceIfNotExisted("config.conf").loadConfig()

        modifyExistedKoinOrCreate {
            modules(bukkitModule)
        }

        commandManager().annotationParser().apply {
            parse(CheckInCommand)
            parse(DailyCommand)
        }

        if (isMenuAvailable) {
            MenuManager.registerButton(DAILY_BUTTON_DESCRIPTOR) { Daily() }
        }
        server.pluginManager.registerEvents(DailyListener, this)
        server.servicesManager.getRegistration(Economy::class.java)?.provider?.also { economy = it }

        redCrossHead // 初始化头颅
    }

    override suspend fun onDisableAsync() {
        Daily.shutdown()
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