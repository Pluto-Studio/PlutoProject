package plutoproject.feature.paper.head

import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser

@Feature(
    id = "head",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class HeadFeature : PaperFeature() {
    private val featureModule = module {
        single<HeadConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        globalKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(HeadCommand)
    }
}
