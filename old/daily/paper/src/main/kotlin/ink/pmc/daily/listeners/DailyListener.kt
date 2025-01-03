package ink.pmc.daily.listeners

import ink.pmc.daily.PLAYER_NOT_CHECKIN_JOIN
import ink.pmc.daily.api.Daily
import ink.pmc.daily.checkCheckInDate
import ink.pmc.framework.concurrent.submitAsyncIO
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

@Suppress("UNUSED")
object DailyListener : Listener, KoinComponent {

    private val daily by inject<Daily>()

    @EventHandler
    fun PlayerJoinEvent.e() {
        fun sendPrompt() {
            player.sendMessage(PLAYER_NOT_CHECKIN_JOIN)
        }

        submitAsyncIO {
            val user = daily.getUser(player) ?: run {
                sendPrompt()
                return@submitAsyncIO
            }
            val now = LocalDate.now()

            if (user.isCheckedInToday()) return@submitAsyncIO
            user.checkCheckInDate()

            if (user.lastCheckInDate?.month != now.month || !user.isCheckedInYesterday()) {
                user.clearAccumulation()
            }
            sendPrompt()
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        daily.unloadUser(player.uniqueId)
    }

}