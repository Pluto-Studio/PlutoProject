package plutoproject.framework.paper.bridge.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.SpawnChangeEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import plutoproject.framework.common.bridge.checkCommonResult
import plutoproject.framework.common.bridge.throwLocalWorldNotFound
import plutoproject.framework.common.bridge.warn
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.paper.bridge.bridgeStub
import plutoproject.framework.paper.bridge.server.localServer
import plutoproject.framework.paper.bridge.world.BackendLocalWorld
import plutoproject.framework.paper.bridge.world.getBridge
import plutoproject.framework.proto.bridge.worldLoad

@Suppress("UnusedReceiverParameter")
object BridgeWorldListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    suspend fun WorldLoadEvent.e() {
        val localWorld = BackendLocalWorld(world, localServer)
        localServer.worlds.add(localWorld)
        val result = bridgeStub.updateWorldInfo(localWorld.createInfo())
        checkCommonResult(result)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    suspend fun SpawnChangeEvent.e() {
        val localWorld = world.getBridge()
            ?: return warn { throwLocalWorldNotFound(world.name) }
        val result = bridgeStub.updateWorldInfo(localWorld.createInfo())
        checkCommonResult(result)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    suspend fun WorldUnloadEvent.e() {
        val localWorld = localServer.getWorld(world.name)
            ?: return warn { throwLocalWorldNotFound(world.name) }
        localServer.worlds.remove(localWorld)
        val result = bridgeStub.unloadWorld(worldLoad {
            server = localServer.id
            world = localWorld.name
        })
        checkCommonResult(result)
    }
}
