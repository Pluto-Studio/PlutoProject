package plutoproject.feature.whitelist_v2.adapter.paper

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import plutoproject.framework.common.util.chat.palettes.mochaMaroon

val VISITOR_CHAT_DENIED = component {
    text("访客状态下无法发送聊天消息") with mochaMaroon
}
