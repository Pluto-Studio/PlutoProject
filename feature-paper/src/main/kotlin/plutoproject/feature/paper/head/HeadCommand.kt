package plutoproject.feature.paper.head

import com.destroystokyo.paper.profile.PlayerProfile
import com.sksamuel.aedile.core.cacheBuilder
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.profile.fetcher.MojangProfileFetcher
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.util.command.ensurePlayer
import plutoproject.framework.paper.util.dsl.ItemStack
import plutoproject.framework.paper.util.hook.vaultHook
import plutoproject.framework.paper.util.inventory.isFull
import plutoproject.framework.paper.util.server
import kotlin.time.Duration.Companion.seconds

private const val HEAD_COST_BYPASS_PERMISSION = "plutoproject.head.bypass_cost"

@Suppress("UNUSED", "UNUSED_PARAMETER")
object HeadCommand : KoinComponent {
    private val config by inject<HeadConfig>()
    private val headCache = cacheBuilder<String, ItemStack>().build()

    private fun createHead(profile: PlayerProfile): ItemStack =
        ItemStack(Material.PLAYER_HEAD) {
            meta {
                this as SkullMeta
                playerProfile = profile
            }
        }

    @Command("head [player]")
    @Permission("plutoproject.head.command.head")
    suspend fun CommandSender.head(@Argument("player", suggestions = "players") player: String?) = ensurePlayer {
        val targetName = (player ?: name).lowercase()
        val cost = config.cost
        val costText = cost.trimmedString()
        val eco = vaultHook?.economy ?: return@ensurePlayer
        val symbol = eco.currencyNameSingular()
        val balance = eco.getBalance(this)

        if (balance < cost && !hasPermission(HEAD_COST_BYPASS_PERMISSION)) {
            send {
                text("货币不足，需要 ") with mochaMaroon
                text("$costText$symbol") with mochaText
            }
            return@ensurePlayer
        }

        val head = headCache.getOrNull(targetName) ?: run {
            val source = if (name != targetName) {
                send {
                    text("正在获取信息，请稍等...") with mochaText
                }
                runCatching {
                    withTimeout(10.seconds) {
                        MojangProfileFetcher.fetchByName(targetName)?.uuid
                    }
                }.onFailure {
                    if (it !is TimeoutCancellationException) throw it
                    send {
                        text("信息获取超时，请重试") with mochaMaroon
                    }
                }.getOrNull()
            } else {
                return@run createHead(playerProfile).also { headCache.put(targetName, it) }
            } ?: run {
                send {
                    text("无法获取玩家 ") with mochaMaroon
                    text("$player ") with mochaText
                    text("的信息") with mochaMaroon
                    newline()
                    text("请检查玩家名是否正确") with mochaSubtext0
                }
                return@ensurePlayer
            }
            val profile = Bukkit.createProfile(source)
            createHead(profile).also { headCache.put(targetName, it) }
        }
        if (inventory.isFull) {
            send {
                text("背包已满，请腾出些空间后再试") with mochaMaroon
            }
            return@ensurePlayer
        }
        inventory.addItem(head)
        send {
            text("已获取 ") with mochaPink
            text("${player ?: name} ") with mochaText
            text("的头颅") with mochaPink
        }
        if (!hasPermission(HEAD_COST_BYPASS_PERMISSION)) {
            eco.withdrawPlayer(this, 3.0)
            send {
                text("消耗 ") with mochaSubtext0
                text("$costText$symbol") with mochaText
            }
        }
    }

    @Suggestions("players")
    fun players(context: CommandContext<CommandSender>, input: CommandInput): List<String> {
        return server.onlinePlayers.map { it.name }
    }
}
