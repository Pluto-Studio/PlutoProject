package ink.pmc.menu.prebuilt.button

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.components.NotAvailable
import ink.pmc.framework.hook.vaultHook
import ink.pmc.framework.trimmed
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.chat.mochaYellow
import ink.pmc.menu.api.dsl.buttonDescriptor
import org.bukkit.Material

val BALANCE_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "menu:balance"
}

@Suppress("FunctionName")
@Composable
fun Balance() {
    val player = LocalPlayer.current
    val economy = vaultHook?.economy
    if (economy == null) {
        NotAvailable(
            material = Material.SUNFLOWER,
            name = component {
                text("货币") with mochaYellow without italic()
            }
        )
        return
    }
    Item(
        material = Material.SUNFLOWER,
        name = component {
            text("货币") with mochaYellow without italic()
        },
        lore = buildList {
            add(component {
                val balance = economy.getBalance(player).trimmed()
                val economySymbol = economy.currencyNameSingular()
                text("你的余额: ") with mochaSubtext0 without italic()
                text("$balance$economySymbol") with mochaText without italic()
            })
            add(component {
                text("可在「礼记」中到访来获取货币") with mochaSubtext0 without italic()
            })
        }
    )
}