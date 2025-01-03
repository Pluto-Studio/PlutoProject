package ink.pmc.protocolchecker

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.framework.chat.mochaText

val VERSION_RANGE
    get() = if (protocolRange.first == protocolRange.last && protocolRange.first.gameVersion.size == 1) {
        protocolRange.first.gameVersion.first()
    } else {
        "${protocolRange.first.gameVersion.first()}~${protocolRange.last.gameVersion.last()}"
    }

val VERSION_NOT_SUPPORTED
    get() = component {
        text("你正在尝试使用不支持的版本加入服务器哦") with mochaMaroon
        newline()
        text("目前服务器支持通过 ") with mochaMaroon
        text(VERSION_RANGE) with mochaText
        text(" 进行游玩") with mochaMaroon
    }

val SERVER_IS_FULL = component {
    text("今日服务器似乎格外热闹...") with mochaMaroon
    newline()
    text("当前服务器已满，请稍后再来") with mochaMaroon
}