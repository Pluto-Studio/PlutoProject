package plutoproject.framework.paper.playerdb

import io.grpc.StatusException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import plutoproject.framework.common.api.playerdb.PlayerDB
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsyncIO
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.playerdb.PlayerDBRpcGrpcKt.PlayerDBRpcCoroutineStub
import plutoproject.framework.proto.playerdb.databaseIdentifier
import java.util.*
import kotlin.time.Duration.Companion.seconds

private lateinit var monitorJob: Job
private val identity = UUID.randomUUID()
private val stub = PlayerDBRpcCoroutineStub(RpcClient.channel)

fun sendUpdateNotification(id: UUID) {
    runAsyncIO {
        stub.notify(databaseIdentifier {
            serverId = identity.toString()
            uuid = id.toString()
        })
    }
}

fun startPlayerDBMonitor() {
    monitorJob = runAsyncIO {
        while (true) {
            try {
                stub.monitorNotify(Empty).also {
                    logger.info("Try to connect player-database monitor stream")
                }.collect {
                    if (it.serverId.convertToUuid() == identity) return@collect
                    val id = it.uuid.convertToUuid()
                    if (!PlayerDB.isLoaded(id)) return@collect
                    PlayerDB.reload(id)
                }
            } catch (e: StatusException) {
                delay(10.seconds)
            }
        }
    }
}

fun stopPlayerDBMonitor() {
    monitorJob.cancel()
}
