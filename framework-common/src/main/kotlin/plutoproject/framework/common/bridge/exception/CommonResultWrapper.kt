package plutoproject.framework.common.bridge.exception

import plutoproject.framework.common.bridge.throwMissingFields
import plutoproject.framework.common.bridge.throwStatusNotSet
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.CommonResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.CommonResult.StatusCase.*

class CommonResultWrapper<T>(
    override val value: T?,
    override val state: CommonResult.StatusCase,
    override val isExceptionOccurred: Boolean
) : StatefulResultWrapper<T, CommonResult.StatusCase> {
    override fun throwException(): Nothing {
        when (state) {
            OK -> error("Unexpected")
            MISSING_FIELDS -> throwMissingFields()
            STATUS_NOT_SET -> throwStatusNotSet("CommonResult")
        }
    }
}
