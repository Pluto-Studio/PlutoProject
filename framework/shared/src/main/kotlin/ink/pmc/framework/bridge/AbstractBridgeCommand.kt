package ink.pmc.framework.bridge

import ink.pmc.advkt.component.*
import ink.pmc.framework.bridge.player.BridgePlayer
import ink.pmc.framework.bridge.server.BridgeServer
import ink.pmc.framework.bridge.world.BridgeLocation
import ink.pmc.framework.bridge.world.BridgeWorld
import ink.pmc.framework.utils.visual.*
import net.kyori.adventure.text.Component
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
abstract class AbstractBridgeCommand<T> {
    abstract fun T.message(component: Component)

    private fun T.send(component: RootComponentKt.() -> Unit) {
        message(RootComponentKt().apply(component).build())
    }

    private fun getServerListMessage(list: Collection<BridgeServer>): Component {
        return component {
            list.forEachIndexed { index, it ->
                text("- ") with mochaSubtext0
                text("${it.id}: ") with mochaText
                text("状态 ") with mochaText
                text("${it.state}") with mochaLavender
                text(", 类型 ") with mochaText
                text("${it.type}") with mochaLavender
                if (it.group != null) {
                    text(", 组 ") with mochaText
                    text(it.group!!.id) with mochaLavender
                }
                text(", 玩家 ") with mochaText
                text(it.playerCount) with mochaLavender
                if (!it.type.isProxy) {
                    text(", 世界 ") with mochaText
                    text(it.worlds.size) with mochaLavender
                }
                if (index != list.indices.last) {
                    newline()
                }
            }
        }
    }

    @Command("bridge list_servers")
    @Permission("bridge.command")
    fun T.listServers() {
        send {
            text("» ") with mochaSubtext0
            text("已注册的服务器") with mochaFlamingo
            newline()
            raw(getServerListMessage(Bridge.servers))
        }
    }

    private suspend fun getPlayerListMessage(list: Collection<BridgePlayer>): Component {
        var component = Component.empty()
        list.forEachIndexed { index, it ->
            var loc: BridgeLocation? = null
            if (!it.serverType.isProxy) {
                loc = it.location.await()
            }
            component = component.append(component {
                text("- ") with mochaSubtext0
                text("${it.name}: ") with mochaText
                text("状态 ") with mochaText
                text("${it.serverState}") with mochaLavender
                text(", 类型 ") with mochaText
                text("${it.serverType}") with mochaLavender
                if (loc != null) {
                    text(", 位置 ") with mochaText
                    text("${loc.world.aliasOrName} ${loc.x.toInt()}, ${loc.y.toInt()}, ${loc.z.toInt()}") with mochaLavender
                }
                if (index != list.indices.last) {
                    newline()
                }
            })
        }
        return component
    }

    @Command("bridge list_player")
    @Permission("bridge.command")
    suspend fun T.listPlayers() {
        send {
            text("» ") with mochaSubtext0
            text("已添加的玩家") with mochaFlamingo
        }
        val list = getPlayerListMessage(Bridge.players)
        send {
            raw(list)
        }
    }

    @Command("bridge player <player> teleport <other>")
    @Permission("bridge.command")
    suspend fun T.teleport(
        player: BridgePlayer,
        other: BridgePlayer
    ) {
        player.teleport(other)
        send {
            text("已将 ") with mochaPink
            text("${player.name} ") with mochaText
            text("传送到 ") with mochaPink
            text(other.name) with mochaText
        }
    }

    @Command("bridge player <player> send_message <message>")
    @Permission("bridge.command")
    suspend fun T.sendMessage(
        player: BridgePlayer,
        message: Component
    ) {
        player.sendMessage(message)
        send {
            text("已向 ") with mochaPink
            text("${player.name} ") with mochaText
            text("发送 ") with mochaPink
            raw(message) with mochaSubtext0
        }
    }

    private fun getWorldListMessage(list: Collection<BridgeWorld>): Component {
        return component {
            list.forEachIndexed { index, it ->
                text("- ") with mochaSubtext0
                text("${it.name}: ") with mochaText
                text("状态 ") with mochaText
                text("${it.serverState}") with mochaLavender
                text(", 类型 ") with mochaText
                text("${it.serverType}") with mochaLavender
                text(", 玩家 ") with mochaText
                text(it.playerCount) with mochaLavender
                text(", 出生点 ") with mochaText
                text("${it.spawnPoint.world.aliasOrName} ${it.spawnPoint.x.toInt()}, ${it.spawnPoint.y.toInt()}, ${it.spawnPoint.z.toInt()}") with mochaLavender
                if (index != list.indices.last) {
                    newline()
                }
            }
        }
    }

    @Command("bridge list_worlds")
    @Permission("bridge.command")
    fun T.listWorlds() {
        send {
            text("» ") with mochaSubtext0
            text("已添加的世界") with mochaFlamingo
            newline()
            raw(getWorldListMessage(Bridge.worlds))
        }
    }
}