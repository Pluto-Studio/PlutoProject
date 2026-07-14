package plutoproject.feature.overload_warning.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.text.mochaYellow

val OVERLOAD_WARNING = component {
    newline()
    text("⚠ ") with mochaYellow
    text("温馨提示 ") with mochaText
    text("»") with mochaSubtext0
    newline()
    text("服务器目前处于严重过载状态，可能会出现生物停滞、不刷怪等现象") with mochaYellow
    newline()
    text("请关闭正在运行的机器、暂缓跑图") with mochaYellow
    newline()
    text("涉及到 TNT 的机器，需尽快关闭以避免损坏") with mochaYellow
    newline()
    text("稳定流畅的游戏体验需大家一同维护，感谢配合") with mochaYellow
    newline()
}
