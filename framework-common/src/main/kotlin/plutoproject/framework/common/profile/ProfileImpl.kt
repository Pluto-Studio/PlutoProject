package plutoproject.framework.common.profile

import plutoproject.framework.common.api.profile.Profile
import java.util.*

class ProfileImpl(override val uuid: UUID, override val name: String) : Profile {
    override val lowercasedName: String
        get() = name.lowercase()
}
