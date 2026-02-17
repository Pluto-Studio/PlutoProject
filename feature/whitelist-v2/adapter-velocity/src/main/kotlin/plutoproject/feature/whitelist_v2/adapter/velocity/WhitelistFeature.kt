package plutoproject.feature.whitelist_v2.adapter.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import net.luckperms.api.LuckPermsProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.whitelist_v2.adapter.common.commonModule
import plutoproject.feature.whitelist_v2.adapter.velocity.commands.MigratorCommand
import plutoproject.feature.whitelist_v2.adapter.velocity.commands.WhitelistCommand
import plutoproject.feature.whitelist_v2.adapter.velocity.commands.WhitelistVisitorCommand
import plutoproject.feature.whitelist_v2.adapter.velocity.listeners.PlayerListener
import plutoproject.feature.whitelist_v2.adapter.velocity.listeners.VisitorListener
import plutoproject.feature.whitelist_v2.api.WhitelistService
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server
import java.util.logging.Logger

lateinit var featureLogger: Logger

@Feature(
    id = "whitelist_v2",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class WhitelistFeature : VelocityFeature(), KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val service by inject<WhitelistService>()

    private val configModule = module {
        single<WhitelistConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        // 先只依赖注入 Config，用于下面的 LuckPerms API 检测
        configureKoin {
            modules(configModule)
        }

        val isLuckPermsApiPresent = runCatching { LuckPermsProvider.get() }.isSuccess
        if (config.visitorMode.enable && !isLuckPermsApiPresent) {
            logger.severe("访客模式功能已启用，但未找到 LuckPerms API。模块将不会加载。")
            return
        }

        configureKoin {
            modules(commonModule)
        }

        VisitorState.setEnabled(config.visitorMode.enable)
        featureLogger = logger
        registerCommands()
        server.eventManager.registerSuspend(plugin, PlayerListener)
        server.eventManager.registerSuspend(plugin, VisitorListener)

        service.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        service.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }

    private fun registerCommands() {
        AnnotationParser.parse(WhitelistCommand)
        AnnotationParser.parse(WhitelistVisitorCommand)
        if (config.enableMigrator) {
            AnnotationParser.parse(MigratorCommand)
        }
    }
}
