package plutoproject.framework.common.rpc

import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import io.grpc.ServerBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.config.RpcConfig
import java.util.logging.Level

class RpcServerImpl : RpcServer, KoinComponent {
    private val config by inject<RpcConfig>()
    private val serverBuilder = Grpc.newServerBuilderForPort(config.port, InsecureServerCredentials.create())
    private var isRunning = false
    private var _server: Server? = null
    override val server: Server
        get() = checkNotNull(_server) { "Server not running" }

    override fun apply(block: ServerBuilder<*>.() -> Unit) {
        check(!isRunning) { "RPC server already running" }
        block.invoke(serverBuilder)
    }

    override fun start() {
        check(!isRunning) { "RPC server already running" }
        try {
            _server = serverBuilder
                .intercept(InternalErrorInterceptor)
                .build()
                .start()
            isRunning = true
            logger.info("Running gRPC server at ${config.port}")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to launch gRPC server", e)
        }
    }

    override fun stop() {
        check(isRunning) { "RPC server is not running" }
        _server?.shutdown()
        _server = null
        isRunning = false
    }
}
