package plutoproject.framework.common.connection

import plutoproject.framework.common.api.connection.CharonFlowConnection
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.util.inject.Koin

private val config by Koin.inject<ExternalConnectionConfig>()
private val mongoConnection by lazy { Koin.get<MongoConnection>() as ExternalConnection }
private val geoIpConnection by lazy { Koin.get<GeoIpConnection>() as ExternalConnection }
private val charonFlowConnection by lazy { Koin.get<CharonFlowConnection>() as ExternalConnection }

fun initializeExternalConnections() {
    if (config.mongo.enabled) mongoConnection
    if (config.geoIp.enabled) geoIpConnection
    if (config.charonflow.enabled) charonFlowConnection
}

fun shutdownExternalConnections() {
    if (config.mongo.enabled) mongoConnection.close()
    if (config.geoIp.enabled) geoIpConnection.close()
    if (config.charonflow.enabled) charonFlowConnection.close()
}
