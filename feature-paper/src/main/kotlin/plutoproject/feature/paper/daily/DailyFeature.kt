package plutoproject.feature.paper.daily

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.core.component.KoinComponent
import org.koin.dsl.module
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.daily.buttons.Daily
import plutoproject.feature.paper.daily.buttons.DailyButtonDescriptor
import plutoproject.feature.paper.daily.commands.DailyCalendarCommand
import plutoproject.feature.paper.daily.commands.CheckInCommand
import plutoproject.feature.paper.daily.listeners.PlayerListener
import plutoproject.feature.paper.daily.repositories.DailyHistoryRepository
import plutoproject.feature.paper.daily.repositories.DailyUserRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

private const val COLLECTION_PREFIX = "daily_"
internal lateinit var featureLogger: Logger

@Feature(
    id = "daily",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class DailyFeature : PaperFeature(), KoinComponent {
    private val featureModule = module {
        single<DailyConfig> { loadConfig(saveConfig()) }
        single<Daily> { DailyImpl() }
        single<DailyUserRepository> {
            DailyUserRepository(MongoConnection.getCollection("${COLLECTION_PREFIX}users"))
        }
        single<DailyHistoryRepository> {
            DailyHistoryRepository(MongoConnection.getCollection("${COLLECTION_PREFIX}history"))
        }
    }

    override fun onEnable() {
        featureLogger = logger
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(CheckInCommand)
        AnnotationParser.parse(DailyCalendarCommand)
        if (isMenuAvailable) {
            MenuManager.registerButton(DailyButtonDescriptor) { Daily() }
        }
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
    }

    override fun onDisable() {
        Daily.shutdown()
    }
}
