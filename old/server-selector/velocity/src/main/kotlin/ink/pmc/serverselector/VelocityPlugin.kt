package ink.pmc.serverselector

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.google.inject.Inject
import com.sksamuel.hoplite.PropertySource
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import ink.pmc.framework.options.OptionsManager
import ink.pmc.framework.command.annotationParser
import ink.pmc.framework.command.commandManager
import ink.pmc.framework.config.preconfiguredConfigLoaderBuilder
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.platform.saveResourceIfNotExisted
import ink.pmc.serverselector.commands.LobbyCommand
import ink.pmc.serverselector.listeners.AutoJoinListener
import org.koin.dsl.module
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

lateinit var dataFolder: File

@Suppress("UNUSED", "UNUSED_PARAMETER")
class VelocityPlugin @Inject constructor(private val spc: SuspendingPluginContainer) {
    private val velocityModule = module {
        single<VelocityServerSelectorConfig> {
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

    init {
        spc.initialize(this)
    }

    @Inject
    fun serverSelector(server: ProxyServer, logger: Logger, @DataDirectory dataDirectoryPath: Path) {
        dataFolder = dataDirectoryPath.toFile()
        modifyExistedKoinOrCreate {
            modules(sharedModule, velocityModule)
        }
        spc.commandManager().annotationParser().apply {
            parse(LobbyCommand)
        }
        OptionsManager.registerOptionDescriptor(AUTO_JOIN_DESCRIPTOR)
        server.eventManager.registerSuspend(this, AutoJoinListener)
    }
}