package plutoproject.capability.profile.api

import java.util.*

interface Profile {
    val uuid: UUID
    val name: String
    val lowercasedName: String
}
