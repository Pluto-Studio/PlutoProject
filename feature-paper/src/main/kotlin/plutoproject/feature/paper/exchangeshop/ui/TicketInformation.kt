package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.exchangeshop.ExchangeShopConfig
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.interactive.LocalPlayer
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

private val config by Koin.inject<ExchangeShopConfig>()

@Composable
fun ticketAmount(): Long {
    val player = LocalPlayer.current
    var amount by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val user = ExchangeShop.getUserOrCreate(player)
        while (isActive) {
            amount = user.ticket
            delay(200.milliseconds)
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
            delay(200.milliseconds)
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
            delay(200.milliseconds)
        }
    }

    return interval
}
