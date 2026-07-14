package plutoproject.feature.randomteleport.paper

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.randomteleport.api.paper.RandomTeleportManager
import plutoproject.foundation.common.text.ECONOMY_SYMBOL
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaMauve
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.time.toFormattedString
import plutoproject.foundation.common.text.trimmedString
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.NotAvailable
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.feature.randomteleport.paper.coroutineContext
import plutoproject.foundation.paper.hook.vaultEconomy
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
    get() = (randomTeleportManager.getCooldown(this)?.remainingSeconds ?: 0).toDuration(DurationUnit.SECONDS)

@Composable
@Suppress("FunctionName")
fun RandomTeleport() {
    val player = LocalPlayer.current
    val world = player.world
    val economy = server.vaultEconomy

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
    val cost = randomTeleportManager.getRandomTeleportOptions(world).cost
    val costMessage = "${cost.trimmedString()}$economySymbol"

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
            if (cooldownRemaining == player.cooldownRemaining) continue
            cooldownRemaining = player.cooldownRemaining
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
        !randomTeleportManager.isEnabled(world) -> RandomTeleportState.NOT_AVAILABLE
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
            randomTeleportManager.launch(player, player.world)
            withContext(player.coroutineContext) {
                player.closeInventory()
            }
        }
    )
}
