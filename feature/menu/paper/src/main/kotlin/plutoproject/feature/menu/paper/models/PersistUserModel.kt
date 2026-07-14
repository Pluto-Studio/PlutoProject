package plutoproject.feature.menu.paper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersistUserModel(
    @SerialName("was_opened_before") val wasOpenedBefore: Boolean = false,
    @SerialName("item_given_servers") val itemGivenServers: List<String> = emptyList(),
)
