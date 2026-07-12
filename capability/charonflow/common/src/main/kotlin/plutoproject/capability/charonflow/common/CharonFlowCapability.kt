package plutoproject.capability.charonflow.common

import club.plutoproject.charonflow.CharonFlow
import club.plutoproject.charonflow.dsl.serialization
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.koin.dsl.module
import org.koin.dsl.onClose
import plutoproject.capability.charonflow.api.CharonFlowConnection
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportService
import plutoproject.kernel.api.loadKoinModuleDefinitions

class CharonFlowCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<CharonFlowConfig>()
        context.loadKoinModuleDefinitions(module {
            single { DefaultCharonFlowConnection(config) }.onClose { it?.close() }
            single<CharonFlowConnection> { get<DefaultCharonFlowConnection>() }
        })
        context.services.exportService<CharonFlowConnection>()
    }
}

internal class DefaultCharonFlowConnection(config: CharonFlowConfig) : CharonFlowConnection, AutoCloseable {
    override val client: CharonFlow = CharonFlow.create {
        redisUri = config.redis
        classLoader = this@DefaultCharonFlowConnection::class.java.classLoader
        serialization {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    override fun close() = client.close()
}
