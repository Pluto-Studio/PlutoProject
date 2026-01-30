package plutoproject.feature.velocity.whitelist_v2

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository

@Suppress("UNUSED")
object PlayerListener : KoinComponent {
    private val recordRepo by inject<WhitelistRecordRepository>()

    @Subscribe
    suspend fun LoginEvent.onPlayerLogin() {
        if (!Whitelist.isWhitelisted(player.uniqueId)) {
            result = ResultedEvent.ComponentResult.denied(PLAYER_NOT_WHITELISTED)
            return
        }

        val recordModel = recordRepo.findByUniqueId(player.uniqueId) ?: error("Unexpected")
        if (recordModel.username == player.username) {
            return
        }

        val updatedModel = recordModel.copy(username = player.username)
        recordRepo.saveOrUpdate(updatedModel)
    }
}
