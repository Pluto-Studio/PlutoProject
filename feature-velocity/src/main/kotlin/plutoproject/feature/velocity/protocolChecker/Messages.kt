package plutoproject.feature.velocity.protocolChecker

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.inject.Koin

private val config by Koin.inject<ProtocolCheckerConfig>()
private val protocolRange by lazy { config.intProtocolRange }

val VERSION_RANGE =
    if (protocolRange.first == protocolRange.last && protocolRange.first.toGameVersions().size == 1) {
        protocolRange.first.toGameVersions().first()
    } else {
        "${protocolRange.first.toGameVersions().first()}~${protocolRange.last.toGameVersions().last()}"
    }

val VERSION_NOT_SUPPORTED = component {
    text("你正在尝试使用不支持的版本加入服务器哦") with mochaMaroon
    newline()
    text("目前服务器支持通过 ") with mochaMaroon
    text(VERSION_RANGE) with mochaText
    text(" 进行游玩") with mochaMaroon
}
