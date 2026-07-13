package plutoproject.feature.head.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.koin.dsl.module
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*

@Feature(
    id = "head",
    platform = Platform.PAPER,
    requiredCapabilities = ["profile", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class HeadFeature : RuntimeModule {
    private lateinit var config: HeadConfig
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        config = ConfigLoaderBuilder.empty()
            .withClassLoader(HeadFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow()
        context.loadKoinModuleDefinitions(module {
            single { config }
        })
    }

    override suspend fun onEnable(context: ModuleContext) {
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, HeadCommand)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
    }
}
