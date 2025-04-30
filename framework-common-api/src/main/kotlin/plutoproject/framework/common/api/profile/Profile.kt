package plutoproject.framework.common.api.profile

import java.util.*

interface Profile {
    val uuid: UUID
    val name: String
    val lowercasedName: String
}
