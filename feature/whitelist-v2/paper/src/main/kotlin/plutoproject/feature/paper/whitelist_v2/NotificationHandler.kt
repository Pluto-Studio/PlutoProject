package plutoproject.feature.paper.whitelist_v2

import club.plutoproject.charonflow.Subscription
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.whitelist_v2.infra.messaging.VISITOR_NOTIFICATION_TOPIC
import plutoproject.feature.whitelist_v2.infra.messaging.VisitorNotification
import plutoproject.framework.common.api.connection.CharonFlowConnection

private lateinit var subscription: Subscription

private object NotificationConfig : KoinComponent {
    val config by inject<WhitelistConfig>()
}

internal suspend fun subscribeNotificationTopic() {
    subscription = CharonFlowConnection.client.subscribe(
        VISITOR_NOTIFICATION_TOPIC,
        VisitorNotification::class,
    ) { notification ->
        if (notification.joinedServer != NotificationConfig.config.serverId) {
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
