package plutoproject.feature.whitelist.common

import kotlinx.serialization.Serializable
import plutoproject.foundation.common.serialization.UuidAsByteArraySerializer
import java.util.UUID

// 访客加入时代理端发送给后端的通知
@Serializable
data class VisitorNotification(
    val uniqueId: @Serializable(UuidAsByteArraySerializer::class) UUID,
    val username: String,
    val joinedServer: String,
)
