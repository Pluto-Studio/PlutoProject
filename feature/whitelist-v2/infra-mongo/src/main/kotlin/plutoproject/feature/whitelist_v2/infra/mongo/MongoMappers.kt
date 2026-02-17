package plutoproject.feature.whitelist_v2.infra.mongo

import plutoproject.feature.whitelist_v2.core.VisitorRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistOperator
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistOperatorDocument
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistOperatorDocumentType
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistRecordDocument
import plutoproject.feature.whitelist_v2.infra.mongo.model.VisitorRecordDocument
import plutoproject.framework.common.util.network.parseInetSocketAddress
import plutoproject.framework.common.util.network.toHostPortString
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.time.Duration.Companion.milliseconds

internal fun WhitelistRecordDocument.toDomain(): WhitelistRecordData {
    return WhitelistRecordData(
        uniqueId = uniqueId,
        username = username,
        granter = granter.toDomain(),
        createdAt = createdAt,
        joinedAsVisitorBefore = joinedAsVisitorBefore,
        isMigrated = isMigrated,
        isRevoked = isRevoked,
        revoker = revoker?.toDomain(),
        revokeReason = revokeReason,
        revokeAt = revokeAt,
    )
}

internal fun WhitelistRecordData.toDocument(): WhitelistRecordDocument {
    return WhitelistRecordDocument(
        uniqueId = uniqueId,
        username = username,
        granter = granter.toDocument(),
        createdAt = createdAt,
        joinedAsVisitorBefore = joinedAsVisitorBefore,
        isMigrated = isMigrated,
        isRevoked = isRevoked,
        revoker = revoker?.toDocument(),
        revokeReason = revokeReason,
        revokeAt = revokeAt,
    )
}

internal fun VisitorRecordDocument.toDomain(): VisitorRecordData {
    return VisitorRecordData(
        uniqueId = uniqueId,
        ipAddress = InetAddress.getByAddress(ipAddress.ipBinary),
        virtualHost = virtualHost.parseInetSocketAddress(),
        visitedAt = visitedAt,
        createdAt = createdAt,
        duration = durationMillis.milliseconds,
        visitedServers = visitedServers,
    )
}

internal fun VisitorRecordData.toDocument(): VisitorRecordDocument {
    val (high, low) = ipAddress.toLongs()
    val version = if (ipAddress is Inet6Address) 6 else 4

    return VisitorRecordDocument(
        uniqueId = uniqueId,
        ipAddress = plutoproject.feature.whitelist_v2.infra.mongo.model.IpAddressDocument(
            ip = ipAddress.hostAddress,
            ipBinary = ipAddress.address,
            ipVersion = version,
            ipHigh = high,
            ipLow = low,
        ),
        virtualHost = virtualHost.toHostPortString(),
        visitedAt = visitedAt,
        createdAt = createdAt,
        durationMillis = duration.inWholeMilliseconds,
        visitedServers = visitedServers,
    )
}

internal fun WhitelistOperatorDocument.toDomain(): WhitelistOperator {
    return when (type) {
        WhitelistOperatorDocumentType.CONSOLE -> WhitelistOperator.Console
        WhitelistOperatorDocumentType.ADMINISTRATOR -> WhitelistOperator.Administrator(
            uniqueId = administrator ?: error("Administrator UUID is null"),
        )
    }
}

internal fun WhitelistOperator.toDocument(): WhitelistOperatorDocument {
    return when (this) {
        WhitelistOperator.Console -> WhitelistOperatorDocument(type = WhitelistOperatorDocumentType.CONSOLE)
        is WhitelistOperator.Administrator -> WhitelistOperatorDocument(
            type = WhitelistOperatorDocumentType.ADMINISTRATOR,
            administrator = uniqueId,
        )
    }
}
