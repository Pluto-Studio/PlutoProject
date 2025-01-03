package plutoproject.framework.velocity.options.proto

import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsyncIO
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.options.OptionsRpcGrpcKt.OptionsRpcCoroutineImplBase
import plutoproject.framework.proto.options.OptionsUpdateNotifyOuterClass.OptionsUpdateNotify
import plutoproject.framework.proto.options.optionsUpdateNotify
import java.util.*

object OptionsRpc : OptionsRpcCoroutineImplBase() {
    private val id: UUID = UUID.randomUUID()
    private val broadcast = MutableSharedFlow<OptionsUpdateNotify>()

    override suspend fun notifyOptionsUpdate(request: OptionsUpdateNotify): Empty {
        val player = request.player.convertToUuid()
        if (OptionsManager.isPlayerLoaded(player)) {
            OptionsManager.reloadOptions(player)
        }
        broadcast.emit(request)
        return Empty
    }

    override fun monitorOptionsUpdate(request: Empty): Flow<OptionsUpdateNotify> {
        return broadcast
    }

    fun notifyBackendContainerUpdate(player: UUID) = runAsyncIO {
        broadcast.emit(optionsUpdateNotify {
            serverId = id.toString()
            this.player = player.toString()
        })
    }
}
