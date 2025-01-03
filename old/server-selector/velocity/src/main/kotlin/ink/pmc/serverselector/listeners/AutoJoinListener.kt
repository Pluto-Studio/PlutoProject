package ink.pmc.serverselector.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import ink.pmc.framework.options.OptionsManager
import ink.pmc.framework.platform.proxy
import ink.pmc.serverselector.AUTO_JOIN_DESCRIPTOR
import ink.pmc.serverselector.storage.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
object AutoJoinListener : KoinComponent {
    private val userRepo by inject<UserRepository>()

    @Subscribe(order = PostOrder.FIRST)
    suspend fun PlayerChooseInitialServerEvent.e() {
        val uuid = player.uniqueId
        val options = OptionsManager.getOptions(uuid) ?: return
        val entry = options.getEntry(AUTO_JOIN_DESCRIPTOR) ?: return
        if (!entry.value) return
        val userModel = userRepo.find(uuid) ?: return
        val server = proxy.getServer(userModel.previouslyJoinedServer).getOrNull() ?: return
        setInitialServer(server)
    }
}