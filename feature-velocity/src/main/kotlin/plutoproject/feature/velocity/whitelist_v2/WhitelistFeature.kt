package plutoproject.feature.velocity.whitelist_v2

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.hook.WhitelistHookType
import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "whitelist_v2",
    platform = Platform.VELOCITY,
    dependencies = [Dependency(id = "whitelist", required = false)]
)
@Suppress("UNUSED")
class WhitelistFeature : VelocityFeature() {
    override fun onEnable() {
        configureKoin {
            modules(whitelistCommonModule)
        }
        server.eventManager.registerSuspend(plugin, PlayerListener)
        Whitelist.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        Whitelist.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }
}
