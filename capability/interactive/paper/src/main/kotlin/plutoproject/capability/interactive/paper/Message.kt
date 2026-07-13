package plutoproject.capability.interactive.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import plutoproject.foundation.common.text.mochaMaroon
import plutoproject.foundation.common.text.mochaSubtext0

val UI_RENDER_FAILED = component {
    text("渲染菜单时出现异常") with mochaMaroon
    newline()
    text("这是一个服务器内部问题，请将其报告给管理组以便我们尽快解决") with mochaSubtext0
}

val UI_INTERACTION_FAILED = component {
    text("处理交互时出现服务器内部错误") with mochaMaroon
    newline()
    text("请将其反馈给管理组以便我们尽快解决") with mochaSubtext0
}
