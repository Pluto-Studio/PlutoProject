package plutoproject.feature.joinquitmessage.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Feature(id = "join_quit_message", platform = Platform.VELOCITY)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class JoinQuitMessageFeature : RuntimeModule {
    private var listener: JoinQuitListener? = null
    private lateinit var config: JoinQuitMessageConfig

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        config = ConfigLoaderBuilder.empty()
            .withClassLoader(JoinQuitMessageFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        listener = JoinQuitListener(context.proxyServer, config).also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext
        listener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        listener = null
    }
}
