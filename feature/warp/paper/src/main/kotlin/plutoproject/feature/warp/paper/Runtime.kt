package plutoproject.feature.warp.paper

import plutoproject.feature.warp.paper.profileLookup

import plutoproject.feature.warp.paper.warpManager

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Player
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.profile.api.Profile
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.foundation.common.time.LocalTimeZone
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.TimeZone
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.optionals.getOrNull

internal val moduleContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val moduleScope
    get() = currentModuleContext().coroutineScope
internal val server: Server
    get() = moduleContext.plugin.server
internal val serverName: String
    get() = currentModuleContext().services.getService<plutoproject.capability.serveridentifier.api.ServerIdentifier>()
        .identifierOrThrow()
internal val warpManager: WarpManager
    get() = koinGet()
internal val teleportManager: TeleportManager
    get() = currentModuleContext().services.getService()
internal val databasePersist: DatabasePersist
    get() = currentModuleContext().services.getService()
internal val profileLookup: ProfileLookup
    get() = currentModuleContext().services.getService()
internal val Player.coroutineContext: CoroutineContext
    get() = moduleContext.plugin.minecraftDispatcher
internal val World.aliasOrName: String
    get() = currentModuleContext().services.getService<WorldAlias>().getAliasOrName(this)
internal val Player.timezone: TimeZone
    get() = address?.let { address ->
        currentModuleContext().services.getService<GeoIpConnection>().database
            .tryCity(address.address)
            .getOrNull()
            ?.location
            ?.timeZone
            ?.let(TimeZone::getTimeZone)
    } ?: LocalTimeZone

internal fun Player.startScreen(screen: InteractiveScreen) {
    currentModuleContext().services.getService<GuiManager>().startScreen(this, screen)
}

internal suspend fun OfflinePlayer.lookupProfile(): Profile? = profileLookup.lookupByUuid(uniqueId)
