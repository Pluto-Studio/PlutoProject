package ink.pmc.essentials.button

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.DEFAULT_ECONOMY_SYMBOL
import ink.pmc.essentials.RANDOM_TELEPORT_COST_BYPASS
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.components.NotAvailable
import ink.pmc.framework.concurrent.sync
import ink.pmc.framework.hook.vaultHook
import ink.pmc.framework.time.formatDuration
import ink.pmc.framework.trimmed
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaMauve
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.buttonDescriptor
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val RANDOM_TELEPORT_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "essentials:random_teleport"
}

// 可用，货币不足，该世界不可用，冷却中
private enum class RandomTeleportState {
    AVAILABLE, COIN_NOT_ENOUGH, NOT_AVAILABLE, IN_COOLDOWN
}

private val Player.cooldownRemaining: Duration
    get() = (RandomTeleportManager.getCooldown(this)?.remainingSeconds ?: 0).toDuration(DurationUnit.SECONDS)

@Composable
@Suppress("FunctionName")
fun RandomTeleport() {
    val player = LocalPlayer.current
    val world = player.world
    val economy = vaultHook?.economy

    if (economy == null) {
        NotAvailable(
            material = Material.AMETHYST_SHARD,
            name = component {
                text("神奇水晶") with mochaMauve without italic()
            }
        )
        return
    }

    val economySymbol = economy.currencyNameSingular() ?: DEFAULT_ECONOMY_SYMBOL
    val balance = economy.getBalance(player)
    val requirement = RandomTeleportManager.getRandomTeleportOptions(world).cost

    val teleportCost = RandomTeleportManager.getRandomTeleportOptions(player.world).cost.trimmed()
    val teleportCostMessage = "$teleportCost$economySymbol"
    var cooldownRemaining by remember { mutableStateOf(player.cooldownRemaining) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cooldownRemaining = player.cooldownRemaining
        }
    }

    val state = when {
        cooldownRemaining > ZERO -> RandomTeleportState.IN_COOLDOWN
        !player.hasPermission(RANDOM_TELEPORT_COST_BYPASS) && balance < requirement -> RandomTeleportState.COIN_NOT_ENOUGH
        !RandomTeleportManager.isEnabled(world) -> RandomTeleportState.NOT_AVAILABLE
        else -> RandomTeleportState.AVAILABLE
    }

    Item(
        material = Material.AMETHYST_SHARD,
        name = component {
            text("神奇水晶") with mochaMauve without italic()
        },
        lore = when (state) {
            RandomTeleportState.AVAILABLE -> buildList {
                add(component {
                    text("具有魔力的紫水晶") with mochaSubtext0 without italic()
                })
                add(component {
                    text("可以带你去世界上的另一个角落") with mochaSubtext0 without italic()
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("进行随机传送 ") with mochaText without italic()
                    text("($teleportCostMessage)") with mochaSubtext0 without italic()
                })
            }

            RandomTeleportState.NOT_AVAILABLE -> buildList {
                add(component {
                    text("该世界未启用随机传送") with mochaSubtext0 without italic()
                })
            }

            RandomTeleportState.COIN_NOT_ENOUGH -> buildList {
                add(component {
                    text("货币不足") with mochaSubtext0 without italic()
                })
                add(component {
                    text("进行随机传送需要 ") with mochaSubtext0 without italic()
                    text(teleportCostMessage) with mochaText without italic()
                })
            }

            RandomTeleportState.IN_COOLDOWN -> buildList {
                add(component {
                    text("传送冷却中...") with mochaSubtext0 without italic()
                })
                add(component {
                    text("还剩 ") with mochaSubtext0 without italic()
                    text(cooldownRemaining.formatDuration()) with mochaText without italic()
                })
            }
        },
        modifier = Modifier.clickable {
            if (state != RandomTeleportState.AVAILABLE) return@clickable
            if (clickType != ClickType.LEFT) return@clickable
            RandomTeleportManager.launch(player, player.world)
            sync {
                player.closeInventory()
            }
        }
    )
}