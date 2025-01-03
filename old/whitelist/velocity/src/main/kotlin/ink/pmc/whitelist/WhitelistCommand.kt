package ink.pmc.whitelist

import com.velocitypowered.api.command.CommandSource
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.framework.player.uuid
import ink.pmc.framework.player.profile.MojangProfileFetcher
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaPink
import ink.pmc.framework.chat.mochaText
import ink.pmc.whitelist.models.AuthType
import ink.pmc.whitelist.models.WhitelistModel
import ink.pmc.whitelist.models.WhitelistState
import ink.pmc.whitelist.models.createWhitelistModel
import ink.pmc.whitelist.repositories.MemberRepository
import ink.pmc.whitelist.repositories.WhitelistRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
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
                MojangProfileFetcher.fetch(name)
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
        repo.deleteById(model.id.uuid)
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

    @Command("whitelist import")
    @Permission("whitelist.command")
    suspend fun CommandSource.import() {
        send {
            text("正在从 Member 系统导入数据...") with mochaText
        }
        val members = get<MemberRepository>().list()
        members.filter {
            it.authType == AuthType.OFFICIAL
                    && it.whitelistStatus == WhitelistState.WHITELISTED
                    && it.isHidden?.not() ?: true
        }.map {
            WhitelistModel(it.id, it.rawName.let { name ->
                if (name.startsWith(".")) name.substring(1) else name
            }, it.createdAt)
        }.forEach {
            if (repo.findById(it.id.uuid) != null) return
            repo.saveOrUpdate(it)
        }
        send {
            text("导入完成，共导入 ") with mochaPink
            text("${members.size} ") with mochaText
            text("个条目") with mochaPink
        }
    }
}