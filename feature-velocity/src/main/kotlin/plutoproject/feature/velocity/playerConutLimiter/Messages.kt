package plutoproject.feature.velocity.playerConutLimiter

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon

val SERVER_IS_FULL = component {
    text("今日服务器似乎格外热闹...") with mochaMaroon
    newline()
    text("当前服务器已满，请稍后再来") with mochaMaroon
}
