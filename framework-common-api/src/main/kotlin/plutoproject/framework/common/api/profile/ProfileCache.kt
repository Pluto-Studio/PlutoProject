package plutoproject.framework.common.api.profile

import plutoproject.framework.common.util.inject.inlinedGet
import java.util.*

interface ProfileCache {
    companion object : ProfileCache by inlinedGet()

    suspend fun getByName(name: String): CachedProfile?

    suspend fun getByUuid(uuid: UUID): CachedProfile?
}
