package ink.pmc.serverselector

import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.time.zoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

private const val GAME_MIDNIGHT_OFFSET = 18000L
private const val SYNC_CYCLE_SECS = 18
private const val PER_SYNC_GAME_SYNC = 24000 / (86400 / SYNC_CYCLE_SECS)
private val SERVER_FOUND_TIME = ZonedDateTime.of(
    2024, 2, 13, 3, 0, 0, 0, ZoneId.of("Asia/Shanghai")
)

private val timeSyncJobs = mutableMapOf<Player, Job>()

fun Player.startTimeSync() {
    val tz = zoneId // cache
    syncTime(tz)
    timeSyncJobs[this] = submitAsync {
        while (true) {
            delay(SYNC_CYCLE_SECS.seconds)
            syncTime(tz)
        }
    }
}

fun Player.stopTimeSync() {
    runCatching {
        timeSyncJobs[this]?.cancel()
    }
}

fun stopTimeSyncJobs() {
    timeSyncJobs.values.forEach {
        runCatching {
            it.cancel()
        }
    }
}

fun Player.syncTime(tz: ZoneId) {
    val time = realWorldTimeToGameTime(tz) + getServerFoundGameTimeOffset(tz)
    setPlayerTime(time, false)
}

private fun realWorldTimeToGameTime(tz: ZoneId): Long {
    val now = ZonedDateTime.now(tz)
    val midnight = now.toLocalDate().atStartOfDay()
    val secondsSinceMidnight = Duration.between(midnight, now).seconds
    return (secondsSinceMidnight / SYNC_CYCLE_SECS) * PER_SYNC_GAME_SYNC + GAME_MIDNIGHT_OFFSET
}

private fun getServerFoundGameTimeOffset(tz: ZoneId): Long {
    val now = ZonedDateTime.now(tz)
    val foundTime = SERVER_FOUND_TIME.withZoneSameInstant(tz)
    val daysSinceFound = Duration.between(foundTime, now).toDays()
    return daysSinceFound * 24000
}