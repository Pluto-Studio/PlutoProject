package plutoproject.capability.geoip.api

import com.maxmind.geoip2.DatabaseReader

interface GeoIpConnection {
    val database: DatabaseReader
}
