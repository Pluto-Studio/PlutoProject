package plutoproject.feature.velocity.whitelist_v2

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookType
import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.feature.velocity.whitelist_v2.commands.MigratorCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistCommand
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

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
        configureKoin {
            modules(whitelistCommonModule, featureModule)
        }
        registerCommands()
        server.eventManager.registerSuspend(plugin, PlayerListener)
        Whitelist.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        Whitelist.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }

    private fun registerCommands() {
        AnnotationParser.parse(WhitelistCommand)
        if (config.enableMigrator) {
            AnnotationParser.parse(MigratorCommand)
        }
    }
}
