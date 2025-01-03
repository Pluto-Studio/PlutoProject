package plutoproject.framework.common.options.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.framework.common.api.options.PlayerOptions

internal fun PlayerOptions.toModel(): PlayerOptionsModel {
    return PlayerOptionsModel(player.toString(), entries.map { it.toModel() })
}

@Serializable
data class PlayerOptionsModel(
    @SerialName("_id") val id: String,
    val entries: List<OptionEntryModel>
)
