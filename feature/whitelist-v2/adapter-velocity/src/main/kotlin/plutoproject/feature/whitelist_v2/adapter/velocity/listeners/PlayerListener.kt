package plutoproject.feature.whitelist_v2.adapter.velocity.listeners

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.InheritanceNode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.whitelist_v2.adapter.common.impl.KnownVisitors
import plutoproject.feature.whitelist_v2.adapter.velocity.*
import plutoproject.feature.whitelist_v2.api.WhitelistService
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.framework.common.api.connection.GeoIpConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Suppress("UNUSED")
object PlayerListener : KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val whitelistRecordRepository by inject<WhitelistRecordRepository>()
    private val service by inject<WhitelistService>()
    private val knownVisitors by inject<KnownVisitors>()
    private val luckpermsApi = LuckPermsProvider.get()

    @Subscribe
    suspend fun LoginEvent.onPlayerLogin() {
        if (!service.isWhitelisted(player.uniqueId)) {
            if (VisitorState.isVisitorModeEnabled) {
                knownVisitors.add(player.uniqueId)

                val visitorGroup = config.visitorMode.visitorPermissionGroup
                val group = luckpermsApi.groupManager.getGroup(visitorGroup)
                val user = luckpermsApi.userManager.getUser(player.uniqueId)

                if (group == null) {
                    featureLogger.severe("没有在 LuckPerms 中找到名为 $visitorGroup 的组，权限组正确配置了吗？")
                    player.disconnect(ERROR_OCCURRED_WHILE_HANDLE_VISITOR_CONNECTION)
                    knownVisitors.remove(player.uniqueId)
                    return
                }
                if (user == null) {
                    featureLogger.severe("无法获取访客 ${player.username} (UUID=${player.uniqueId}) 的 LuckPerms 用户对象，这似乎不应该发生...")
                    player.disconnect(ERROR_OCCURRED_WHILE_HANDLE_VISITOR_CONNECTION)
                    knownVisitors.remove(player.uniqueId)
                    return
                }

                // 移除 default 组
                val defaultGroup = luckpermsApi.groupManager.getGroup("default")
                if (defaultGroup != null) {
                    val defaultNode = InheritanceNode.builder(defaultGroup).build()
                    user.data().remove(defaultNode)
                }

                // 添加 visitor 组
                val inheritanceNode = InheritanceNode.builder(group).build()
                user.data().add(inheritanceNode)

                // 设置主组为 visitor
                val primaryGroupResult = user.setPrimaryGroup(visitorGroup)
                luckpermsApi.userManager.saveUser(user)

                if (!primaryGroupResult.wasSuccessful()) {
                    featureLogger.severe("在尝试为访客 ${player.username} (UUID=${player.uniqueId}) 切换到 $visitorGroup 组时失败。")
                    featureLogger.severe("修改主组是否成功：${primaryGroupResult.wasSuccessful()}")
                    user.data().remove(inheritanceNode)
                    player.disconnect(ERROR_OCCURRED_WHILE_HANDLE_VISITOR_CONNECTION)
                    knownVisitors.remove(player.uniqueId)
                    return
                }

                featureLogger.info("已将访客 ${player.username} (UUID=${player.uniqueId}) 切换到 $visitorGroup 组。")

                VisitorListener.recordVisitorSession(
                    player.uniqueId,
                    player.remoteAddress.address,
                    player.virtualHost.getOrNull(),
                )

                logVisitorLogin(
                    player.uniqueId,
                    player.username,
                    player.remoteAddress.address,
                    player.virtualHost.getOrNull(),
                )
                return
            }
            result = ResultedEvent.ComponentResult.denied(PLAYER_NOT_WHITELISTED)
            return
        }

        val record = whitelistRecordRepository.findByUniqueId(player.uniqueId) ?: error("Unexpected")
        if (record.username == player.username) {
            return
        }

        val usernameChanged = record.apply {
            changeUsername(player.username)
        }
        whitelistRecordRepository.saveOrUpdate(usernameChanged)
    }

    private fun logVisitorLogin(
        uuid: UUID,
        username: String,
        ip: InetAddress,
        virtualHost: InetSocketAddress?,
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
            "访客进入: UUID=$uuid, 用户名=$username, IP=$ip, 连接地址=${virtualHost ?: "未知"}, 位置=$location",
        )
    }
}
