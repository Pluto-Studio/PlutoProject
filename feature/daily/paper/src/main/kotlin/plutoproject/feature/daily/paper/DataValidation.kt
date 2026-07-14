package plutoproject.feature.daily.paper

import plutoproject.feature.daily.api.paper.DailyUser
import java.time.LocalDate

fun LocalDate.isCheckInDateValid(): Boolean {
    return !isAfter(LocalDate.now())
}

suspend fun DailyUser.checkCheckInDate() {
    if (lastCheckInDate?.isCheckInDateValid() == false) {
        resetCheckInTime()
        featureLogger.warning("Abnormal check-in date detected for ${player.name}, reset to default")
        featureLogger.warning("Is the system time incorrect?")
    }
}
