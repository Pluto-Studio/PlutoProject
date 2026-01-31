package plutoproject.framework.common.connection

import club.plutoproject.charonflow.CharonFlow
import club.plutoproject.charonflow.dsl.serialization
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.framework.common.api.connection.CharonFlowConnection

class CharonFlowConnectionImpl : CharonFlowConnection, ExternalConnection, KoinComponent {
    private val config by lazy { get<ExternalConnectionConfig>().charonflow }

    init {
        check(config.enabled) { "CharonFlow external connection is disabled in configuration." }
    }

    override val client: CharonFlow = connectCharonFlow()

    private fun connectCharonFlow(): CharonFlow = CharonFlow.create {
        redisUri = config.redis
        serialization {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    override fun close() {
        client.close()
    }
}
