package ink.pmc.daily

import ink.pmc.daily.api.DailyUser
import java.time.LocalDate

fun LocalDate.isCheckInDateValid(): Boolean {
    return !isAfter(LocalDate.now())
}

suspend fun DailyUser.checkCheckInDate() {
    if (lastCheckInDate?.isCheckInDateValid() == false) {
        resetCheckInTime()
        plugin.logger.warning("Abnormal check-in date detected for ${player.name}, reset to default")
        plugin.logger.warning("Is the system time incorrect?")
    }
}