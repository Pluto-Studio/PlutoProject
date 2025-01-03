package plutoproject.framework.paper.options

import io.grpc.StatusException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import plutoproject.framework.common.api.options.OptionsManager
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.options.OptionsRpcGrpcKt
import plutoproject.framework.proto.options.optionsUpdateNotify
import java.util.*
import kotlin.time.Duration.Companion.seconds

private lateinit var monitorJob: Job
private val id: UUID = UUID.randomUUID()
private val stub = OptionsRpcGrpcKt.OptionsRpcCoroutineStub(RpcClient.channel)

fun sendContainerUpdateNotify(player: UUID) {
    runAsync {
        stub.notifyOptionsUpdate(optionsUpdateNotify {
            serverId = id.toString()
            this.player = player.toString()
        })
    }
}

fun startOptionsMonitor() {
    monitorJob = runAsync {
        while (true) {
            try {
                stub.monitorOptionsUpdate(Empty).also {
                    logger.info("Try to connect options monitor stream")
                }.collect {
                    val serverId = it.serverId.convertToUuid()
                    val player = it.player.convertToUuid()
                    if (Bukkit.getPlayer(player) == null || serverId == id) {
                        return@collect
                    }
                    OptionsManager.reloadOptions(player)
                }
            } catch (e: StatusException) {
                delay(10.seconds)
            }
        }
    }
}

fun stopOptionsMonitor() {
    monitorJob.cancel()
}
