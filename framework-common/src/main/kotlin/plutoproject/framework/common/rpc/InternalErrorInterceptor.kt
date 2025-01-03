package plutoproject.framework.common.rpc

import io.grpc.*
import java.util.logging.Level

object InternalErrorInterceptor : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val listener = next.startCall(call, headers)
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                runCatching {
                    super.onHalfClose()
                }.onFailure {
                    logger.log(
                        Level.SEVERE,
                        "Exception in RPC call: ${call.methodDescriptor.fullMethodName}",
                        it
                    )
                    call.close(Status.INTERNAL.withDescription("Internal server error"), headers)
                }
            }
        }
    }
}
