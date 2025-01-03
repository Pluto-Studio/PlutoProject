package plutoproject.framework.common.api.bridge

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaText

object MessageConstants {
    val playerNotFound = component {
        text("玩家 ") with mochaMaroon
        text("<player> ") with mochaText
        text("未找到") with mochaMaroon
    }

    val serverNotFound = component {
        text("服务器 ") with mochaMaroon
        text("<server> ") with mochaText
        text("未找到") with mochaMaroon
    }
}