package ink.pmc.essentials

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.google.inject.Inject
import com.sksamuel.hoplite.PropertySource
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import ink.pmc.essentials.velocity.EssentialsProxyConfig
import ink.pmc.essentials.velocity.listeners.MessageListener
import ink.pmc.framework.config.preconfiguredConfigLoaderBuilder
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.platform.saveResourceIfNotExisted
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

lateinit var dataFolder: File

@Suppress("UNUSED", "UNUSED_PARAMETER")
class VelocityPlugin @Inject constructor(spc: SuspendingPluginContainer) : KoinComponent {
    private val velocityModule = module {
        single<EssentialsProxyConfig> {
            preconfiguredConfigLoaderBuilder()
                .addPropertySource(
                    PropertySource.file(
                        saveResourceIfNotExisted(
                            VelocityPlugin::class.java,
                            "proxy_config.conf",
                            File(dataFolder, "config.conf")
                        )
                    )
                )
                .build()
                .loadConfigOrThrow()
        }
    }
    private val config by inject<EssentialsProxyConfig>()

    init {
        spc.initialize(this)
    }

    @Inject
    fun essentials(server: ProxyServer, logger: Logger, @DataDirectory dataDirectoryPath: Path) {
        dataFolder = dataDirectoryPath.toFile()
        modifyExistedKoinOrCreate {
            modules(velocityModule)
        }
        if (config.message.enabled) {
            server.eventManager.registerSuspend(this, MessageListener)
        }
    }
}