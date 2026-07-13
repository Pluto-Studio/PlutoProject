package plutoproject.feature.daily.paper

import kotlinx.coroutines.CoroutineScope
import org.bukkit.Server
import org.bukkit.entity.Player
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.foundation.common.time.LocalTimeZone
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.TimeZone
import kotlin.jvm.optionals.getOrNull

internal val paperContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val server: Server
    get() = paperContext.plugin.server
internal val moduleScope: CoroutineScope
    get() = paperContext.coroutineScope
internal val Player.timezone: TimeZone
    get() = address?.let { address ->
        currentModuleContext().services.getService<GeoIpConnection>().database
            .tryCity(address.address)
            .getOrNull()
            ?.location
            ?.timeZone
            ?.let(TimeZone::getTimeZone)
    } ?: LocalTimeZone
