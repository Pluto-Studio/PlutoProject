package plutoproject.feature.warp.paper.buttons

import plutoproject.feature.warp.paper.warpManager

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.screens.DefaultSpawnPickerScreen
import plutoproject.foundation.common.text.replaceInComponent
import plutoproject.foundation.common.text.mochaFlamingo
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.feature.warp.paper.aliasOrName

val SpawnButtonDescriptor = ButtonDescriptor {
    id = "essentials:spawn"
}

private sealed class PreferredSpawnState {
    data object Loading : PreferredSpawnState()
    class Ready(val spawn: Warp) : PreferredSpawnState()
    data object None : PreferredSpawnState()
}

@Composable
@Suppress("FunctionName")
fun Spawn() {
    val navigator = LocalNavigator.currentOrThrow
    val player = LocalPlayer.current
    var preferredSpawnState by remember { mutableStateOf<PreferredSpawnState>(PreferredSpawnState.Loading) }

    LaunchedEffect(Unit) {
        val spawn = warpManager.getPreferredSpawn(player)
        val defaultSpawn = warpManager.getDefaultSpawn()
        preferredSpawnState = when {
            spawn != null -> PreferredSpawnState.Ready(spawn)
            defaultSpawn != null -> PreferredSpawnState.Ready(defaultSpawn)
            else -> PreferredSpawnState.None
        }
    }

    Item(
        material = Material.COMPASS,
        name = component {
            text("伊始之处") with mochaFlamingo
        },
        lore = when (preferredSpawnState) {
            is PreferredSpawnState.Loading -> buildList {
                add(Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0))
            }

            is PreferredSpawnState.Ready -> {
                val spawn = (preferredSpawnState as PreferredSpawnState.Ready).spawn
                val lore = buildList {
                    add(component {
                        text("旅途的起点") with mochaSubtext0
                    })
                    add(Component.empty())
                    add(component {
                        text("左键 ") with mochaLavender
                        text("回到主城") with mochaText
                    })
                    add(component {
                        text("右键 ") with mochaLavender
                        text("设置首选主城") with mochaText
                    })
                }
                val name = when (spawn.alias) {
                    null -> component { text(spawn.name) with mochaText }
                    else -> component { text(spawn.alias!!) with mochaText }
                }
                val loc = spawn.let {
                    val world = it.location.world.aliasOrName
                    val x = it.location.blockX
                    val y = it.location.blockY
                    val z = it.location.blockZ
                    component { text("$world $x, $y, $z") with mochaSubtext0 }
                }
                lore.replaceInComponent("<spawn>", name).replaceInComponent("<loc>", loc)
            }

            is PreferredSpawnState.None -> buildList {
                add(component {
                    text("你还没有首选的主城") with mochaSubtext0
                })
                add(component {
                    text("右键点击来设置") with mochaSubtext0
                })
            }
        }.toList(),
        modifier = Modifier.clickable {
            when (clickType) {
                ClickType.LEFT -> {
                    val spawn = (preferredSpawnState as? PreferredSpawnState.Ready)?.spawn ?: return@clickable
                    spawn.teleport(player)
                }

                ClickType.RIGHT -> navigator.push(DefaultSpawnPickerScreen())
                else -> {}
            }
        }
    )
}
