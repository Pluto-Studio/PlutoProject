package plutoproject.framework.velocity.playerdb.proto

import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.component.KoinComponent
import plutoproject.framework.common.api.playerdb.PlayerDB
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsyncIO
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.playerdb.PlayerDBRpcGrpcKt.PlayerDBRpcCoroutineImplBase
import plutoproject.framework.proto.playerdb.PlayerDBRpcOuterClass.DatabaseIdentifier
import plutoproject.framework.proto.playerdb.databaseIdentifier
import java.util.*

object PlayerDBRpc : PlayerDBRpcCoroutineImplBase(), KoinComponent {
    private val identity = UUID.randomUUID()
    private val broadcast = MutableSharedFlow<DatabaseIdentifier>()

    override suspend fun notify(request: DatabaseIdentifier): Empty {
        val id = request.uuid.convertToUuid()
        if (PlayerDB.isLoaded(id)) {
            PlayerDB.reload(id)
        }
        broadcast.emit(request)
        return Empty
    }

    override fun monitorNotify(request: Empty): Flow<DatabaseIdentifier> {
        return broadcast
    }

    fun notifyDatabaseUpdate(player: UUID) = runAsyncIO {
        broadcast.emit(databaseIdentifier {
            serverId = identity.toString()
            uuid = player.toString()
        })
    }
}
