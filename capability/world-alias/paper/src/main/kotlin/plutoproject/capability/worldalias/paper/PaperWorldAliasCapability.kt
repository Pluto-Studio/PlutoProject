package plutoproject.capability.worldalias.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.koin.dsl.module
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.capability.worldalias.paper.config.WorldAliasConfig
import plutoproject.capability.worldalias.paper.worldalias.WorldAliasImpl
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportServiceFromKoin
import plutoproject.kernel.api.loadKoinModuleDefinitions

@Capability(id = "world-alias", platform = Platform.PAPER)
class PaperWorldAliasCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<WorldAliasConfig>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<WorldAlias> { WorldAliasImpl(get()) }
        })
        context.services.exportServiceFromKoin<WorldAlias>()
    }
}
