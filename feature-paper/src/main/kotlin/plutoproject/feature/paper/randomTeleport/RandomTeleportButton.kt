package plutoproject.feature.paper.randomTeleport

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaMauve
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.chat.toFormattedString
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.NotAvailable
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.hook.vaultHook
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val RandomTeleportButtonDescriptor = ButtonDescriptor {
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
                text("神奇水晶") with mochaMauve
            }
        )
        return
    }

    val economySymbol = economy.currencyNameSingular() ?: ECONOMY_SYMBOL
    val balance = economy.getBalance(player)
    val cost = RandomTeleportManager.getRandomTeleportOptions(world).cost
    val costMessage = "${cost}$economySymbol"

    var cooldownRemaining by remember { mutableStateOf(player.cooldownRemaining) }
    var cooldownAnimationProgress by rememberSaveable { mutableStateOf(0) }
    val cooldownAnimationIcon = when (cooldownAnimationProgress) {
        0 -> Material.SMALL_AMETHYST_BUD
        1 -> Material.MEDIUM_AMETHYST_BUD
        2 -> Material.LARGE_AMETHYST_BUD
        else -> error("Unexpected")
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cooldownRemaining = player.cooldownRemaining
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (cooldownAnimationProgress < 2) {
                cooldownAnimationProgress++
            } else {
                cooldownAnimationProgress = 0
            }
        }
    }

    val state = when {
        cooldownRemaining > ZERO -> RandomTeleportState.IN_COOLDOWN
        !player.hasPermission(RANDOM_TELEPORT_COST_BYPASS_PERMISSION) && balance < cost -> RandomTeleportState.COIN_NOT_ENOUGH
        !RandomTeleportManager.isEnabled(world) -> RandomTeleportState.NOT_AVAILABLE
        else -> RandomTeleportState.AVAILABLE
    }

    Item(
        material = if (state == RandomTeleportState.IN_COOLDOWN) cooldownAnimationIcon else Material.AMETHYST_SHARD,
        name = component {
            text("神奇水晶") with mochaMauve
        },
        lore = when (state) {
            RandomTeleportState.AVAILABLE -> buildList {
                add(component {
                    text("具有魔力的紫水晶") with mochaSubtext0
                })
                add(component {
                    text("可以带你去世界上的另一个角落") with mochaSubtext0
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    if (cost > 0.0) {
                        text("进行随机传送 ") with mochaText
                        text("($costMessage)") with mochaSubtext0
                    } else {
                        text("进行随机传送") with mochaText
                    }
                })
            }

            RandomTeleportState.NOT_AVAILABLE -> buildList {
                add(component {
                    text("该世界未启用随机传送") with mochaSubtext0
                })
            }

            RandomTeleportState.COIN_NOT_ENOUGH -> buildList {
                add(component {
                    text("货币不足") with mochaSubtext0
                })
                add(component {
                    text("进行随机传送需要 ") with mochaSubtext0
                    text(costMessage) with mochaText
                })
            }

            RandomTeleportState.IN_COOLDOWN -> buildList {
                add(component {
                    text("正在凝聚力量...") with mochaSubtext0
                })
                add(component {
                    text("还剩 ") with mochaSubtext0
                    text(cooldownRemaining.toFormattedString()) with mochaText
                })
            }
        },
        modifier = Modifier.clickable {
            if (state != RandomTeleportState.AVAILABLE) return@clickable
            if (clickType != ClickType.LEFT) return@clickable
            RandomTeleportManager.launch(player, player.world)
            withSync {
                player.closeInventory()
            }
        }
    )
}
