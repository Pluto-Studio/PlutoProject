package plutoproject.feature.paper.whitelist_v2

import club.plutoproject.charonflow.Subscription
import plutoproject.feature.common.whitelist_v2.VISITOR_NOTIFICATION_TOPIC
import plutoproject.feature.common.whitelist_v2.VisitorNotification
import plutoproject.framework.common.api.connection.CharonFlowConnection
import plutoproject.framework.common.util.inject.Koin

private lateinit var subscription: Subscription
private val config by lazy { Koin.get<WhitelistConfig>() }

internal suspend fun subscribeNotificationTopic() {
    subscription = CharonFlowConnection.client.subscribe(
        VISITOR_NOTIFICATION_TOPIC,
        VisitorNotification::class
    ) { notification ->
        if (notification.joinedServer != config.serverId) {
            return@subscribe
        }
        VisitorListener.onVisitorIncoming(notification.uniqueId, notification.username)
    }.getOrThrow()
}

internal suspend fun unsubscribeNotificationTopic() {
    if (!::subscription.isInitialized) {
        return
    }
    subscription.unsubscribe()
}
