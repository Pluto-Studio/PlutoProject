package plutoproject.framework.common.api.profile

import java.util.*

@Suppress("UNUSED")
interface ProfileFetcher {
    val id: String

    suspend fun fetch(name: String): ProfileData?

    suspend fun validate(name: String, uuid: UUID): Boolean

    suspend fun validate(name: String, uuid: String): Boolean
}
