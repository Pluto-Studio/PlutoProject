package plutoproject.framework.velocity.profile

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.profile.ProfileModel
import plutoproject.framework.common.profile.ProfileRepository

@Suppress("UNUSED")
object ProfilePlayerListener : KoinComponent {
    private val repo by inject<ProfileRepository>()

    @Subscribe()
    suspend fun LoginEvent.e() {
        val model = repo.findByUniqueId(player.uniqueId)
        if (model == null) {
            repo.save(ProfileModel(ObjectId(), player.uniqueId, player.username))
            return
        }
        if (model.name == player.username) return
        repo.update(model.copy(name = player.username))
    }
}
