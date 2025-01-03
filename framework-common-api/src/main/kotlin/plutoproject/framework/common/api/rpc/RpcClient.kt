package plutoproject.framework.common.api.rpc

import io.grpc.Channel
import plutoproject.framework.common.util.inject.inlinedGet

interface RpcClient {
    companion object : RpcClient by inlinedGet()

    val channel: Channel

    fun start()

    fun stop()
}
