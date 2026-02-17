package plutoproject.feature.whitelist_v2.adapter.common.impl

import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.api.result.WhitelistGrantResult
import plutoproject.feature.whitelist_v2.api.result.WhitelistRevokeResult
import plutoproject.feature.whitelist_v2.core.usecase.GrantWhitelistUseCase
import plutoproject.feature.whitelist_v2.core.usecase.RevokeWhitelistUseCase
import plutoproject.feature.whitelist_v2.core.VisitorRecordParams as CoreVisitorRecordParams
import plutoproject.feature.whitelist_v2.core.WhitelistOperator as CoreWhitelistOperator
import plutoproject.feature.whitelist_v2.core.WhitelistRevokeReason as CoreWhitelistRevokeReason

internal fun WhitelistOperator.toCore(): CoreWhitelistOperator {
    return when (this) {
        WhitelistOperator.Console -> CoreWhitelistOperator.Console
        is WhitelistOperator.Administrator -> CoreWhitelistOperator.Administrator(uniqueId)
    }
}

internal fun CoreWhitelistOperator.toApi(): WhitelistOperator {
    return when (this) {
        CoreWhitelistOperator.Console -> WhitelistOperator.Console
        is CoreWhitelistOperator.Administrator -> WhitelistOperator.Administrator(uniqueId)
    }
}

internal fun WhitelistRevokeReason.toCore(): CoreWhitelistRevokeReason {
    return when (this) {
        WhitelistRevokeReason.VIOLATION -> CoreWhitelistRevokeReason.VIOLATION
        WhitelistRevokeReason.REQUESTED -> CoreWhitelistRevokeReason.REQUESTED
        WhitelistRevokeReason.OTHER -> CoreWhitelistRevokeReason.OTHER
    }
}

internal fun CoreWhitelistRevokeReason.toApi(): WhitelistRevokeReason {
    return when (this) {
        CoreWhitelistRevokeReason.VIOLATION -> WhitelistRevokeReason.VIOLATION
        CoreWhitelistRevokeReason.REQUESTED -> WhitelistRevokeReason.REQUESTED
        CoreWhitelistRevokeReason.OTHER -> WhitelistRevokeReason.OTHER
    }
}

internal fun VisitorRecordParams.toCore(): CoreVisitorRecordParams {
    return CoreVisitorRecordParams(
        ipAddress = ipAddress,
        virtualHost = virtualHost,
        visitedAt = visitedAt,
        duration = duration,
        visitedServers = visitedServers,
    )
}

internal fun GrantWhitelistUseCase.Result.toApi(): WhitelistGrantResult {
    return when (this) {
        GrantWhitelistUseCase.Result.Ok -> WhitelistGrantResult.Ok
        GrantWhitelistUseCase.Result.AlreadyGranted -> WhitelistGrantResult.AlreadyGranted
    }
}

internal fun RevokeWhitelistUseCase.Result.toApi() : WhitelistRevokeResult {
    return  when(this) {
        RevokeWhitelistUseCase.Result.Ok -> WhitelistRevokeResult.Ok
        RevokeWhitelistUseCase.Result.NotGranted -> WhitelistRevokeResult.NotGranted
    }
}
