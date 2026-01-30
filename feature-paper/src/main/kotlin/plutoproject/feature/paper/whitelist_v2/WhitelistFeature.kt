package plutoproject.feature.paper.whitelist_v2

import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature

@Feature(
    id = "whitelist_v2",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class WhitelistFeature : PaperFeature() {
    override fun onEnable() {
        configureKoin {
            modules(whitelistCommonModule)
        }
    }
}
