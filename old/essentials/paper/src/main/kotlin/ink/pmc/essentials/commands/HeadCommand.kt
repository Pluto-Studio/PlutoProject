package ink.pmc.essentials.commands

import com.destroystokyo.paper.profile.PlayerProfile
import com.sksamuel.aedile.core.cacheBuilder
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.essentials.HEAD_COST_BYPASS
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.economyHook
import ink.pmc.framework.command.ensurePlayer
import ink.pmc.framework.dsl.itemStack
import ink.pmc.framework.inventory.isFull
import ink.pmc.framework.platform.paper
import ink.pmc.framework.player.profile.MojangProfileFetcher
import ink.pmc.framework.trimmed
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaPink
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
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
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED", "UNUSED_PARAMETER")
object HeadCommand : KoinComponent {
    private val config by lazy { get<EssentialsConfig>().head }
    private val headCache = cacheBuilder<String, ItemStack>().build()

    private fun createHead(profile: PlayerProfile): ItemStack {
        return itemStack(Material.PLAYER_HEAD) {
            meta {
                this as SkullMeta
                playerProfile = profile
            }
        }
    }

    @Command("head [player]")
    @Permission("essentials.head")
    suspend fun CommandSender.head(@Argument("player", suggestions = "players") player: String?) = ensurePlayer {
        if (!config.enabled) return@ensurePlayer

        val targetName = (player ?: name).lowercase()
        val cost = config.cost
        val costText = cost.trimmed()
        val eco = economyHook?.economy ?: return@ensurePlayer
        val symbol = eco.currencyNameSingular()
        val balance = eco.getBalance(this)

        if (balance < cost && !hasPermission(HEAD_COST_BYPASS)) {
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
                        MojangProfileFetcher.fetch(targetName)?.uuid
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
        if (!hasPermission(HEAD_COST_BYPASS)) {
            eco.withdrawPlayer(this, 3.0)
            send {
                text("消耗 ") with mochaSubtext0
                text("$costText$symbol") with mochaText
            }
        }
    }

    @Suggestions("players")
    fun players(context: CommandContext<CommandSender>, input: CommandInput): List<String> {
        return paper.onlinePlayers.map { it.name }
    }
}