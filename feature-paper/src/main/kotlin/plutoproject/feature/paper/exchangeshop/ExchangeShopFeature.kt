package plutoproject.feature.paper.exchangeshop

import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class ExchangeShopFeature : PaperFeature() {
    private val featureModule = module {
        single<ExchangeShopConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
    }
}
