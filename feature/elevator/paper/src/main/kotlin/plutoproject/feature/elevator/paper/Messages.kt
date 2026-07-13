package plutoproject.feature.elevator.paper

import ink.pmc.advkt.component.text
import ink.pmc.advkt.title.*
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaYellow
import kotlin.time.Duration.Companion.seconds

val ELEVATOR_GO_UP_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("电梯上行 ") with mochaYellow
        text("(<curr>/<total>)") with mochaSubtext0
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val ELEVATOR_GO_DOWN_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("电梯下行 ") with mochaYellow
        text("(<curr>/<total>)") with mochaSubtext0
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}
