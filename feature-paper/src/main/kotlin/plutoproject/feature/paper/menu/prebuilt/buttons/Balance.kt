package plutoproject.feature.paper.menu.prebuilt.buttons

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import org.bukkit.Material
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.chat.palettes.mochaYellow
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.NotAvailable
import plutoproject.framework.paper.util.hook.vaultHook

val BalanceButtonDescriptor = ButtonDescriptor {
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
                text("货币") with mochaYellow
            }
        )
        return
    }
    Item(
        material = Material.SUNFLOWER,
        name = component {
            text("货币") with mochaYellow
        },
        lore = buildList {
            add(component {
                val balance = economy.getBalance(player).trimmedString()
                val economySymbol = economy.currencyNameSingular()
                text("你的余额: ") with mochaSubtext0
                text("$balance$economySymbol") with mochaText
            })
            add(component {
                text("可在「礼记」中到访来获取货币") with mochaSubtext0
            })
        }
    )
}
