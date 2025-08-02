package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.raw
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.kyori.adventure.text.Component
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.interactive.LocalPlayer
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

private val config by Koin.inject<ExchangeShopConfig>()

@Composable
fun ticketAmount(): Long {
    val player = LocalPlayer.current
    var amount by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val user = ExchangeShop.getUserOrCreate(player)
        while (isActive) {
            amount = user.ticket
            delay(1.seconds)
        }
    }

    return amount
}

@Composable
fun ticketRecoveryTime(): Instant? {
    val player = LocalPlayer.current
    var time: Instant? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        val user = ExchangeShop.getUserOrCreate(player)
        while (isActive) {
            time = user.scheduledTicketRecoveryTime
            delay(1.seconds)
        }
    }

    return time
}

@Composable
fun ticketRecoveryInterval(): Duration? {
    val time = ticketRecoveryTime()
    var interval: Duration? by remember { mutableStateOf(null) }

    LaunchedEffect(time) {
        while (isActive) {
            interval = time?.let { Duration.between(Instant.now(), it) }
            delay(1.seconds)
        }
    }

    return interval
}

private fun Duration.toMMSSFormat(): String {
    val totalSeconds = this.seconds
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun ticketRecoveryIntervalDisplay(): Component {
    val amount = ticketAmount()
    val recoveryInterval = ticketRecoveryInterval()
    return if (amount < config.ticket.recoveryCap && recoveryInterval != null) {
        val intervalDisplay = Duration.ofSeconds(recoveryInterval.seconds + 1).toMMSSFormat()
        EXCHANGE_SHOP_BUTTON_LORE_TICKET_RECOVERY_INTERVAL.replace("<interval>", intervalDisplay)
    } else {
        EXCHANGE_SHOP_BUTTON_LORE_TICKET_FULL
    }
}

val ShopItem.priceDisplay: Component
    get() = component {
        if (isFree) {
            raw(SHOP_ITEM_LORE_PRICE_FREE)
        }
        if (hasMoneyCost) {
            raw(SHOP_ITEM_LORE_PRICE_COST.replace("<cost>", price.stripTrailingZeros().toPlainString()))
            if (hasTicketConsumption) {
                raw(SHOP_ITEM_LORE_PRICE_AND)
            }
        }
        if (hasTicketConsumption) {
            raw(SHOP_ITEM_LORE_PRICE_TICKET.replace("<ticket>", ticketConsumption))
        }
        if (!isMultipleQuantity) {
            raw(SHOP_ITEM_LORE_QUANTITY_SINGLE)
        } else {
            raw(SHOP_ITEM_LORE_QUANTITY_MULTIPLE.replace("<quantity>", quantity))
        }
    }
