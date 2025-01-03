package plutoproject.framework.paper.interactive

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0

val uiRenderFailed = component {
    text("渲染菜单时出现异常") with mochaMaroon
    newline()
    text("这是一个服务器内部问题，请将其报告给管理组以便我们尽快解决") with mochaSubtext0
}
