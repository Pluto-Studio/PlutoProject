package plutoproject.framework.common.api.rpc

import io.grpc.Server
import io.grpc.ServerBuilder
import plutoproject.framework.common.util.inject.inlinedGet

interface RpcServer {
    companion object : RpcServer by inlinedGet()

    val server: Server

    fun apply(block: ServerBuilder<*>.() -> Unit)

    fun start()

    fun stop()
}
