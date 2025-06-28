package plutoproject.feature.paper.dev_watermark

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.buildinfo.BuildInfo
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText

val DEV_WATERMARK = component {
    if (!BuildInfo.isStable) {
        text("开发版本，不代表最终品质") with mochaText
        text(" (${BuildInfo.gitCommit}@${BuildInfo.gitBranch})") with mochaSubtext0
    } else {
        text("开发版水印已启用，但未在运行开发版") with mochaMaroon
    }
}
