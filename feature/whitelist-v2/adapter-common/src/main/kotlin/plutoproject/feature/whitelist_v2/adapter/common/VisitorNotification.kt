package plutoproject.feature.whitelist_v2.adapter.common

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.UuidAsByteArraySerializer
import java.util.UUID

// 访客加入时代理端发送给后端的通知
@Serializable
data class VisitorNotification(
    val uniqueId: @Serializable(UuidAsByteArraySerializer::class) UUID,
    val username: String,
    val joinedServer: String,
)
