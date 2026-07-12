package plutoproject.capability.profile.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import plutoproject.capability.profile.common.ProfileDocument
import plutoproject.capability.profile.common.ProfileRepository

class ProfilePlayerListener(
    private val scope: CoroutineScope,
    private val repository: ProfileRepository,
) {
    @Subscribe(order = PostOrder.LAST)
    fun onPostLogin(event: PostLoginEvent) {
        scope.launch {
            val player = event.player
            val document = repository.findByUniqueId(player.uniqueId)
            if (document == null) {
                repository.save(ProfileDocument(uuid = player.uniqueId, name = player.username))
            } else if (document.name != player.username) {
                repository.update(document.copy(name = player.username))
            }
        }
    }
}
