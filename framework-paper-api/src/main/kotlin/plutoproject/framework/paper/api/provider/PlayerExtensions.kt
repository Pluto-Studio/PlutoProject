package plutoproject.framework.paper.api.provider

import org.bukkit.entity.Player
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.util.time.LocalTimeZone
import java.util.*
import kotlin.jvm.optionals.getOrNull

val Player.timezone: TimeZone
    get() = address?.let { address ->
        GeoIpConnection.database.tryCity(address.address).getOrNull()?.location?.timeZone?.let { TimeZone.getTimeZone(it) }
    } ?: LocalTimeZone
