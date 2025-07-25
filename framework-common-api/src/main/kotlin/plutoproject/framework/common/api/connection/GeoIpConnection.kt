package plutoproject.framework.common.api.connection

import com.maxmind.geoip2.DatabaseReader
import plutoproject.framework.common.util.inject.Koin

/**
 * GeoIP 数据库外部链接。
 */
interface GeoIpConnection {
    companion object : GeoIpConnection by Koin.get()

    /**
     * 此数据库文件的 [DatabaseReader] 实例。
     */
    val database: DatabaseReader
}
