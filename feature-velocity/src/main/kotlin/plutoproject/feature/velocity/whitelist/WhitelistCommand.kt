package plutoproject.feature.velocity.whitelist

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.profile.fetcher.MojangProfileFetcher
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.data.convertToUuid
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object WhitelistCommand : KoinComponent {
    private val repo by inject<WhitelistRepository>()

    @Command("whitelist add <name>")
    @Permission("whitelist.command")
    suspend fun CommandSource.add(@Argument("name") name: String) {
        repo.findByName(name)?.let { model ->
            send {
                text("玩家 ") with mochaMaroon
                text("${model.rawName} ") with mochaText
                text("已经拥有白名单") with mochaMaroon
            }
            return
        }
        send {
            text("正在获取数据，请稍等...") with mochaText
        }
        val data = try {
            withTimeout(10.seconds) {
                MojangProfileFetcher.fetchByName(name)
            }
        } catch (e: TimeoutCancellationException) {
            send {
                text("数据获取超时，请重试") with mochaMaroon
            }
            return
        }
        val profileData = data ?: run {
            send {
                text("未获取到玩家 ") with mochaMaroon
                text("$name ") with mochaText
                text("的数据，请检查玩家名是否正确") with mochaMaroon
            }
            return
        }
        val model = createWhitelistModel(profileData.uuid, profileData.name)
        repo.saveOrUpdate(model)
        send {
            text("已为玩家 ") with mochaPink
            text("${profileData.name} ") with mochaText
            text("添加白名单") with mochaPink
        }
    }

    @Command("whitelist lookup <name>")
    @Permission("whitelist.command")
    suspend fun CommandSource.lookup(@Argument("name") name: String) {
        val model = repo.findByName(name) ?: run {
            send {
                text("未查询到名为 ") with mochaMaroon
                text("$name ") with mochaText
                text("的玩家") with mochaMaroon
            }
            return
        }
        send {
            text("已查询到名为 ") with mochaPink
            text("${model.rawName} ") with mochaText
            text("的玩家") with mochaPink
        }
    }

    @Command("whitelist remove <name>")
    @Permission("whitelist.command")
    suspend fun CommandSource.remove(@Argument("name") name: String) {
        val model = repo.findByName(name) ?: run {
            send {
                text("名为 ") with mochaMaroon
                text("$name ") with mochaText
                text("的玩家未获得白名单") with mochaMaroon
            }
            return
        }
        repo.deleteById(model.id.convertToUuid())
        send {
            text("已经移除玩家 ") with mochaPink
            text("${model.rawName} ") with mochaText
            text("的白名单") with mochaPink
        }
    }

    @Command("whitelist statistic")
    @Permission("whitelist.command")
    suspend fun CommandSource.statistic() {
        val count = repo.count()
        send {
            text("当前有 ") with mochaText
            text("$count ") with mochaLavender
            text("位玩家获得了白名单") with mochaText
        }
    }
}
