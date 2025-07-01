package plutoproject.feature.paper.sitV2

import org.koin.dsl.module
import plutoproject.feature.paper.api.sitV2.Sit
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser

@Feature(
    id = "sit_v2",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class SitV2Feature : PaperFeature() {
    private val featureModule = module {
        single<Sit> { SitImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(SitCommand)
    }
}
