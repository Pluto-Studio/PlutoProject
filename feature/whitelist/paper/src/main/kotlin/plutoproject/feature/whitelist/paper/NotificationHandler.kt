package plutoproject.feature.whitelist.paper

import club.plutoproject.charonflow.Subscription
import plutoproject.capability.charonflow.api.CharonFlowConnection
import plutoproject.feature.whitelist.common.VISITOR_NOTIFICATION_TOPIC
import plutoproject.feature.whitelist.common.VisitorNotification
internal class NotificationHandler(
    private val connection: CharonFlowConnection,
    private val config: WhitelistConfig,
    private val visitorListener: VisitorListener,
) {
    private var subscription: Subscription? = null

    suspend fun subscribe() {
        subscription = connection.client.subscribe(
            VISITOR_NOTIFICATION_TOPIC,
            VisitorNotification::class,
        ) { notification ->
            if (notification.joinedServer == config.serverId) {
                visitorListener.onVisitorIncoming(notification.uniqueId, notification.username)
            }
        }.getOrThrow()
    }

    suspend fun unsubscribe() {
        subscription?.unsubscribe()
        subscription = null
    }
}
