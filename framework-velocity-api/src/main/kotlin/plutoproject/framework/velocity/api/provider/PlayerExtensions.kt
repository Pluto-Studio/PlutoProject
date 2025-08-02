package plutoproject.framework.velocity.api.provider

import com.velocitypowered.api.proxy.Player
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.util.time.LocalTimeZone
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
inline val Player.timezone: TimeZone
    get() = GeoIpConnection.database.tryCity(remoteAddress.address)
        .getOrNull()?.location?.timeZone?.let { TimeZone.getTimeZone(it) } ?: LocalTimeZone
