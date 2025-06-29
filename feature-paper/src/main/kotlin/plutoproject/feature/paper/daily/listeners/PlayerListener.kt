package plutoproject.feature.paper.daily.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.daily.NOT_CHECKED_IN_TODAY
import plutoproject.feature.paper.daily.checkCheckInDate
import java.time.LocalDate

@Suppress("UNUSED")
object PlayerListener : Listener, KoinComponent {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        fun sendPrompt() {
            player.sendMessage(NOT_CHECKED_IN_TODAY)
        }

        val user = Daily.getUser(player) ?: run {
            sendPrompt()
            return
        }
        val now = LocalDate.now()

        if (user.isCheckedInToday()) return
        user.checkCheckInDate()

        if (user.lastCheckInDate?.month != now.month || !user.isCheckedInYesterday()) {
            user.clearAccumulation()
        }
        sendPrompt()
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        Daily.unloadUser(player.uniqueId)
    }
}
