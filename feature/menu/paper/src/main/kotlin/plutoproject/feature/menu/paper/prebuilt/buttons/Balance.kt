package plutoproject.feature.menu.paper.prebuilt.buttons

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import org.bukkit.Material
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.text.mochaYellow
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.NotAvailable
import plutoproject.feature.menu.paper.server
import plutoproject.foundation.paper.hook.vaultEconomy

val BalanceButtonDescriptor = ButtonDescriptor {
    id = "menu:balance"
}

@Suppress("FunctionName")
@Composable
fun Balance() {
    val player = LocalPlayer.current
    val economy = server.vaultEconomy
    if (economy == null) {
        NotAvailable(
            material = Material.SUNFLOWER,
            name = component {
                text("星币") with mochaYellow
            }
        )
        return
    }
    Item(
        material = Material.SUNFLOWER,
        name = component {
            text("星币") with mochaYellow
        },
        lore = buildList {
            add(component {
                val balance = economy.getBalance(player).toBigDecimal().stripTrailingZeros().toPlainString()
                val economySymbol = economy.currencyNameSingular()
                text("余额: ") with mochaSubtext0
                text("$balance$economySymbol") with mochaText
            })
            add(component {
                text("可在「礼记」中到访以获取星币") with mochaSubtext0
            })
        }
    )
}
