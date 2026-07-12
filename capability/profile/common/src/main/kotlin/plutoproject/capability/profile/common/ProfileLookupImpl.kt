package plutoproject.capability.profile.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import plutoproject.capability.profile.api.Profile
import plutoproject.capability.profile.api.ProfileLookup
import java.util.*

class ProfileLookupImpl(
    private val scope: CoroutineScope,
    private val repository: ProfileRepository,
    private val fetcher: MojangProfileFetcher,
) : ProfileLookup {
    override suspend fun lookupByUuid(uuid: UUID, requestApi: Boolean): Profile? {
        repository.findByUniqueId(uuid)?.let { return it.toProfile() }
        if (!requestApi) return null

        return fetcher.fetchByUuid(uuid)?.also(::saveLater)?.toProfile()
    }

    override suspend fun lookupByName(name: String, requestApi: Boolean): Profile? {
        repository.findByName(name)?.let { return it.toProfile() }
        if (!requestApi) return null

        return fetcher.fetchByName(name)?.also(::saveLater)?.toProfile()
    }

    override suspend fun validate(uuid: UUID, name: String, requestApi: Boolean): Boolean {
        if (requestApi) updateName(uuid)
        return lookupByUuid(uuid, requestApi = requestApi)?.lowercasedName == name.lowercase()
    }

    private fun saveLater(fetched: FetchedProfile) {
        scope.launch { repository.save(ProfileDocument(uuid = fetched.uuid, name = fetched.name)) }
    }

    private suspend fun updateName(uuid: UUID) {
        val document = repository.findByUniqueId(uuid) ?: return
        val fetched = fetcher.fetchByUuid(uuid) ?: return
        if (document.name != fetched.name) repository.update(document.copy(name = fetched.name))
    }
}

private fun ProfileDocument.toProfile(): Profile = ProfileImpl(uuid, name)

private fun FetchedProfile.toProfile(): Profile = ProfileImpl(uuid, name)
