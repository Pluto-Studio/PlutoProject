package plutoproject.feature.versionchecker.velocity

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import plutoproject.foundation.common.text.*

fun getVersionRange(config: VersionCheckerConfig): String = if (config.supportedGameVersions.size == 1) {
    config.supportedGameVersions.first()
} else {
    "${config.supportedGameVersions.first()}-${config.supportedGameVersions.last()}"
}

fun getVersionNotCompatible(config: VersionCheckerConfig): Component = component {
    text("你正在尝试使用不支持的版本加入") with mochaMaroon
    newline()
    text("目前支持通过 ") with mochaMaroon
    text(getVersionRange(config)) with mochaText
    text(" 进行游玩") with mochaMaroon
}

fun getUnsupportedVersionWarning(config: VersionCheckerConfig): Component = component {
    text("⚠ 警告") with mochaYellow
    newline()
    text("- ") with mochaSubtext0
    text("你正在使用不支持的版本，这可能会导致一些问题") with mochaMaroon
    newline()
    text("- ") with mochaSubtext0
    text("如无法移动，无法正常显示部分内容") with mochaMaroon
    newline()
    text("- ") with mochaSubtext0
    text("目前支持 ") with mochaMaroon
    text(getVersionRange(config)) with mochaText
    text("，请勿反馈使用不支持的版本时遇到的问题") with mochaMaroon
    newline()
    text("[更新前不再显示]") with mochaLavender with showText {
        text("下次更新游戏版本前不再显示") with mochaText
    } with runCommand("/ignore-version-warning")
}

val COMMAND_IGNORE_ENABLED = component {
    text("下次更新游戏版本前不会再显示版本警告") with mochaSubtext0
}

val COMMAND_IGNORE_DISABLED = component {
    text("已重新打开版本警告") with mochaSubtext0
}

val COMMAND_IGNORE_FAILED_NOT_ON_UNSUPPORTED_VERSION = component {
    text("你没有在使用不支持的版本") with mochaMaroon
}

val COMMAND_PLAYERS_ONLY: Component = Component.text("该命令只能由玩家执行", mochaMaroon)
