package plutoproject.feature.velocity.whitelist_v2

import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature

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
    }
}
