package plutoproject.framework.common.bridge

import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.CommonResult
import java.util.logging.Level

fun warn(block: () -> Nothing) {
    runCatching {
        block()
    }.onFailure {
        logger.log(Level.WARNING, "", it)
    }
}

fun throwServerNotFound(id: String): Nothing {
    error("Server not found: $id")
}

fun throwRemoteServerNotFound(id: String): Nothing {
    error("Remote server not found: $id")
}

fun throwRemoteServerOffline(id: String): Nothing {
    error("Remote server offline: $id")
}

fun throwPlayerNotFound(name: String): Nothing {
    error("Player not found: $name")
}

fun throwRemotePlayerNotFound(name: String): Nothing {
    error("Remote player not found: $name")
}

fun throwRemotePlayerOffline(name: String): Nothing {
    error("Remote player offline: $name")
}

fun throwLocalPlayerNotFound(name: String): Nothing {
    error("Local player not found: $name")
}

fun throwWorldNotFound(): Nothing {
    error("World not found")
}

fun throwWorldNotFound(name: String, server: String?): Nothing {
    if (server != null) {
        error("World not found: $name (server: $server)")
    } else {
        error("World not found: $name")
    }
}

fun throwRemoteWorldNotFound(name: String, server: String?): Nothing {
    if (server != null) {
        error("Remote world not found: $name (server: $server)")
    } else {
        error("Remote world not found: $name")
    }
}

fun throwLocalWorldNotFound(name: String): Nothing {
    error("Local world not found: $name")
}

fun throwMissingFields(): Nothing {
    error("Missing fields")
}

fun throwStatusNotSet(name: String): Nothing {
    error("Received a $name without status")
}

fun throwContentNotSet(name: String): Nothing {
    error("Received a $name without content")
}

fun checkCommonResult(result: CommonResult) {
    when (result.statusCase!!) {
        CommonResult.StatusCase.OK -> {}
        CommonResult.StatusCase.MISSING_FIELDS -> warn { throwMissingFields() }
        CommonResult.StatusCase.STATUS_NOT_SET -> warn { throwStatusNotSet("CommonResult") }
    }
}
