package plutoproject.feature.paper.serverSelector

import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import net.kyori.adventure.util.Ticks
import org.bukkit.entity.Player
import plutoproject.feature.common.serverSelector.TELEPORT_FAILED_SOUND
import plutoproject.feature.common.serverSelector.UserRepository
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.util.entity.switchServer

private val userRepo by Koin.inject<UserRepository>()

suspend fun Player.transferServer(id: String) {
    runCatching {
        switchServer(id)
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
    val userModel = userRepo.findOrCreate(uniqueId)
    userRepo.saveOrUpdate(userModel.copy(previouslyJoinedServer = id))
}
