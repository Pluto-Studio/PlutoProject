package ink.pmc.serverselector

import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import ink.pmc.essentials.TELEPORT_FAILED_SOUND
import ink.pmc.essentials.TELEPORT_SUCCEED_SOUND
import ink.pmc.essentials.TELEPORT_SUCCEED_TITLE
import ink.pmc.framework.bridge.player.toBridge
import ink.pmc.framework.inject.koin
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaText
import ink.pmc.serverselector.storage.UserRepository
import net.kyori.adventure.util.Ticks
import org.bukkit.entity.Player

private val userRepo by koin.inject<UserRepository>()

suspend fun Player.transferServer(id: String) {
    runCatching {
        toBridge().switchServer(id)
    }.onFailure {
        showTitle {
            times {
                fadeIn(Ticks.duration(5))
                stay(Ticks.duration(35))
                fadeOut(Ticks.duration(20))
            }
            mainTitle {
                text("传送失败") with mochaMaroon
            }
            subTitle {
                text("请再试一次") with mochaText
            }
        }
        playSound(TELEPORT_FAILED_SOUND)
        return
    }
    val bridge = toBridge()
    bridge.showTitle(TELEPORT_SUCCEED_TITLE)
    bridge.playSound(TELEPORT_SUCCEED_SOUND)
    val userModel = userRepo.findOrCreate(uniqueId)
    userRepo.saveOrUpdate(userModel.copy(previouslyJoinedServer = id))
}