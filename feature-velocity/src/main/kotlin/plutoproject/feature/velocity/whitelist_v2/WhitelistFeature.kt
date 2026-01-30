package plutoproject.feature.velocity.whitelist_v2

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookType
import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.feature.velocity.whitelist_v2.commands.MigratorCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistCommand
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

private const val LEGACY_WHITELIST_FEATURE = "whitelist"

@Feature(
    id = "whitelist_v2",
    platform = Platform.VELOCITY,
    dependencies = [Dependency(id = LEGACY_WHITELIST_FEATURE, required = false)]
)
@Suppress("UNUSED")
class WhitelistFeature : VelocityFeature() {
    override fun onEnable() {
        configureKoin {
            modules(whitelistCommonModule)
        }
        registerCommands()
        server.eventManager.registerSuspend(plugin, PlayerListener)
        Whitelist.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        Whitelist.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }

    private fun registerCommands() {
        // TODO: 配置文件 - 开关迁移功能
        val migratorEnabled = true
        AnnotationParser.parse(WhitelistCommand)
        if (migratorEnabled) {
            if (!FeatureManager.isEnabled(LEGACY_WHITELIST_FEATURE)) {
                logger.severe(LOG_LEGACY_WHITELIST_FEATURE_NOT_ENABLED)
                return
            }
            AnnotationParser.parse(MigratorCommand)
        }
    }
}
