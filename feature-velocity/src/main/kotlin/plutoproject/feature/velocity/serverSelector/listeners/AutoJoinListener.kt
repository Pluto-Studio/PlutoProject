package plutoproject.feature.velocity.serverSelector.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import org.koin.core.component.KoinComponent
import plutoproject.feature.common.serverSelector.AUTO_JOIN_TOGGLE_PERSIST_KEY
import plutoproject.feature.common.serverSelector.PREVIOUSLY_JOINED_SERVER_PERSIST_KEY
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.BooleanTypeAdapter
import plutoproject.framework.common.api.databasepersist.adapters.StringTypeAdapter
import plutoproject.framework.velocity.util.server
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
object AutoJoinListener : KoinComponent {
    @Subscribe(order = PostOrder.FIRST)
    suspend fun PlayerChooseInitialServerEvent.e() {
        val container = DatabasePersist.getContainer(player.uniqueId)
        if (!container.getOrDefault(AUTO_JOIN_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, false)) {
            return
        }
        val previouslyJoinedServer = container.get(PREVIOUSLY_JOINED_SERVER_PERSIST_KEY, StringTypeAdapter) ?: return
        val registeredServer = server.getServer(previouslyJoinedServer).getOrNull() ?: return
        setInitialServer(registeredServer)
    }
}
