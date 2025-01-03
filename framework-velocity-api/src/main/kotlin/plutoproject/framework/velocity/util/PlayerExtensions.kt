package plutoproject.framework.velocity.util

import com.velocitypowered.api.proxy.ConnectionRequestBuilder
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.future.await

suspend fun Player.switchServer(name: String): ConnectionRequestBuilder.Result {
    val registeredServer = server.getServer(name).get()
    val future = createConnectionRequest(registeredServer).connect()
    return future.await()
}
