package plutoproject.framework.paper.api.profile

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import plutoproject.framework.common.api.profile.Profile
import plutoproject.framework.common.api.profile.ProfileLookup

suspend fun Player.lookupProfile(): Profile = ProfileLookup.lookupByUuid(uniqueId)!!

suspend fun OfflinePlayer.lookupProfile(): Profile? =
    name?.let { ProfileLookup.lookupByName(it) } ?: ProfileLookup.lookupByUuid(uniqueId)
