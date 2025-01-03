package ink.pmc.essentials.button

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.essentials.screens.warp.DefaultSpawnPickerScreen
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.chat.replace
import ink.pmc.framework.chat.mochaFlamingo
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.world.aliasOrName
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val SPAWN_BUTTON_DESCRIPTOR = buttonDescriptor {
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
        val spawn = WarpManager.getPreferredSpawn(player)
        val defaultSpawn = WarpManager.getDefaultSpawn()
        preferredSpawnState = when {
            spawn != null -> PreferredSpawnState.Ready(spawn)
            defaultSpawn != null -> PreferredSpawnState.Ready(defaultSpawn)
            else -> PreferredSpawnState.None
        }
    }

    Item(
        material = Material.COMPASS,
        name = component {
            text("伊始之处") with mochaFlamingo without italic()
        },
        lore = when (preferredSpawnState) {
            is PreferredSpawnState.Loading -> buildList {
                add(component {
                    text("正在加载...") with mochaSubtext0 without italic()
                })
            }

            is PreferredSpawnState.Ready -> {
                val spawn = (preferredSpawnState as PreferredSpawnState.Ready).spawn
                val lore = buildList {
                    add(component {
                        text("旅途的起点") with mochaSubtext0 without italic()
                    })
                    add(Component.empty())
                    add(component {
                        text("左键 ") with mochaLavender without italic()
                        text("回到主城") with mochaText without italic()
                    })
                    add(component {
                        text("右键 ") with mochaLavender without italic()
                        text("设置首选主城") with mochaText without italic()
                    })
                }
                val name = when (spawn.alias) {
                    null -> component { text(spawn.name) with mochaText without italic() }
                    else -> component { text(spawn.alias!!) with mochaText without italic() }
                }
                val loc = spawn.let {
                    val world = it.location.world.aliasOrName
                    val x = it.location.blockX
                    val y = it.location.blockY
                    val z = it.location.blockZ
                    component { text("$world $x, $y, $z") with mochaSubtext0 without italic() }
                }
                lore.replace("<spawn>", name).replace("<loc>", loc)
            }

            is PreferredSpawnState.None -> buildList {
                add(component {
                    text("你还没有首选的主城") with mochaSubtext0 without italic()
                })
                add(component {
                    text("右键点击来设置") with mochaSubtext0 without italic()
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