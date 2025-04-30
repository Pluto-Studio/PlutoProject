package plutoproject.feature.velocity.whitelist

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon

val PLAYER_NOT_WHITELISTED = component {
    text("此 ID 未获得白名单") with mochaMaroon
    newline()
    text("若已通过审核，请联系当日的审核员添加") with mochaMaroon
}
