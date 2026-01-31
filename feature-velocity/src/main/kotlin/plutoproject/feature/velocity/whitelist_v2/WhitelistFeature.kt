package plutoproject.feature.velocity.whitelist_v2

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import net.luckperms.api.LuckPermsProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookType
import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.feature.velocity.whitelist_v2.commands.MigratorCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistVisitorCommand
import plutoproject.feature.velocity.whitelist_v2.listeners.PlayerListener
import plutoproject.feature.velocity.whitelist_v2.listeners.VisitorListener
import plutoproject.feature.velocity.whitelist_v2.VisitorState
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
    private val featureModule = module {
        single<WhitelistConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        // 先只依赖注入 Config，用于下面的 LuckPerms API 检测
        configureKoin {
            modules(featureModule)
        }

        val isLuckPermsApiPresent = runCatching { LuckPermsProvider.get() }.isSuccess
        if (config.visitorMode.enable && !isLuckPermsApiPresent) {
            logger.severe("访客模式功能已启用，但未找到 LuckPerms API。模块将不会加载。")
            return
        }

        configureKoin {
            modules(whitelistCommonModule)
        }

        VisitorState.setEnabled(config.visitorMode.enable)
        featureLogger = logger
        registerCommands()
        server.eventManager.registerSuspend(plugin, PlayerListener)
        server.eventManager.registerSuspend(plugin, VisitorListener)
        Whitelist.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        Whitelist.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }

    private fun registerCommands() {
        AnnotationParser.parse(WhitelistCommand)
        AnnotationParser.parse(WhitelistVisitorCommand)
        if (config.enableMigrator) {
            AnnotationParser.parse(MigratorCommand)
        }
    }
}
