package plutoproject.framework.common.profile

import org.bson.types.ObjectId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.profile.Profile
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.api.profile.fetcher.MojangProfileFetcher
import plutoproject.framework.common.util.coroutine.runAsync
import java.util.*

class ProfileLookupImpl : ProfileLookup, KoinComponent {
    private val repo by inject<ProfileRepository>()

    override suspend fun lookupByUuid(uuid: UUID, requestApi: Boolean): Profile? {
        val model = repo.findByUniqueId(uuid)
        if (model != null) {
            return ProfileImpl(model.uuid, model.name)
        }
        if (requestApi) {
            val fetched = MojangProfileFetcher.fetchByUuid(uuid)
            if (fetched != null) {
                runAsync {
                    repo.save(ProfileModel(ObjectId(), fetched.uuid, fetched.name))
                }
                return ProfileImpl(fetched.uuid, fetched.name)
            }
        }
        return null
    }

    override suspend fun lookupByName(name: String, requestApi: Boolean): Profile? {
        val model = repo.findByName(name)
        if (model != null) {
            return ProfileImpl(model.uuid, model.name)
        }
        if (requestApi) {
            val fetched = MojangProfileFetcher.fetchByName(name)
            if (fetched != null) {
                runAsync {
                    repo.save(ProfileModel(ObjectId(), fetched.uuid, fetched.name))
                }
                return ProfileImpl(fetched.uuid, fetched.name)
            }
        }
        return null
    }

    override suspend fun validate(uuid: UUID, name: String, requestApi: Boolean): Boolean {
        if (requestApi) {
            updateName(uuid)
        }
        val profile = lookupByUuid(uuid) ?: return false
        return profile.lowercasedName == name.lowercase()
    }

    private suspend fun updateName(uuid: UUID) {
        val model = repo.findByUniqueId(uuid) ?: return
        val fetched = MojangProfileFetcher.fetchByUuid(uuid) ?: return
        if (model.name == fetched.name) return
        repo.update(model.copy(name = fetched.name))
    }
}
