package plutoproject.framework.common.profile

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.profile.CachedProfile
import plutoproject.framework.common.api.profile.MojangProfileFetcher
import plutoproject.framework.common.api.profile.ProfileCache
import java.util.*

class ProfileCacheImpl : ProfileCache, KoinComponent {
    private val repo by inject<ProfileCacheRepository>()

    override suspend fun getByName(name: String): CachedProfile? {
        return repo.findByName(name) ?: MojangProfileFetcher.fetch(name)?.let {
            CachedProfile(it.name, it.name.lowercase(), it.uuid).also { profile ->
                repo.saveOrUpdate(profile)
            }
        }
    }

    override suspend fun getByUuid(uuid: UUID): CachedProfile? {
        return repo.findByUuid(uuid)
    }
}
