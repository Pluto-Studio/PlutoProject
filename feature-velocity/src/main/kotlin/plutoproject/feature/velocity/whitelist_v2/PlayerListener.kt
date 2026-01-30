package plutoproject.feature.velocity.whitelist_v2

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.whitelist_v2.WhitelistImpl
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.framework.common.api.connection.GeoIpConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
object PlayerListener : KoinComponent {
    private val recordRepo by inject<WhitelistRecordRepository>()
    private val whitelist by lazy { get<Whitelist>() as WhitelistImpl }

    @Subscribe
    suspend fun LoginEvent.onPlayerLogin() {
        if (!whitelist.isWhitelisted(player.uniqueId)) {
            if (VisitorState.isVisitorModeEnabled) {
                whitelist.addKnownVisitor(player.uniqueId)
                player.sendMessage(PLAYER_VISITOR_WELCOME)
                // TODO: 切换玩家权限组到 visitor

                VisitorListener.recordVisitorSession(
                    player.uniqueId,
                    player.remoteAddress.address,
                    player.virtualHost.getOrNull()
                )

                logVisitorLogin(
                    player.uniqueId,
                    player.username,
                    player.remoteAddress.address,
                    player.virtualHost.getOrNull()
                )
                return
            }
            result = ResultedEvent.ComponentResult.denied(PLAYER_NOT_WHITELISTED)
            return
        }

        val recordModel = recordRepo.findByUniqueId(player.uniqueId) ?: error("Unexpected")
        if (recordModel.username == player.username) {
            return
        }

        val updatedModel = recordModel.copy(username = player.username)
        recordRepo.saveOrUpdate(updatedModel)
    }

    private fun logVisitorLogin(
        uuid: java.util.UUID,
        username: String,
        ip: InetAddress,
        virtualHost: InetSocketAddress?
    ) {
        val location = try {
            val response = GeoIpConnection.database.city(ip)
            val city = response.city.name ?: "未知城市"
            val subdivision = response.mostSpecificSubdivision.name ?: "未知省份"
            val country = response.country.name ?: "未知国家"
            "$city, $subdivision, $country"
        } catch (e: Exception) {
            "未知位置"
        }

        featureLogger.info(
            "访客进入: UUID=$uuid, 用户名=$username, IP=$ip, 连接地址=${virtualHost ?: "未知"}, 位置=$location"
        )
    }
}
