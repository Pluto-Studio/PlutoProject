package plutoproject.feature.paper.gm

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.mochaFlamingo
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
import plutoproject.framework.common.util.chat.palettes.mochaText

val SURVIVAL = Component.text("生存模式")

val CREATIVE = Component.text("创造模式")

val ADVENTURE = Component.text("冒险模式")

val SPECTATOR = Component.text("旁观模式")

val COMMAND_GM = component {
    text("已将游戏模式切换为 ") with mochaPink
    text("<gamemode>") with mochaText
}

val COMMAND_GM_OTHER = component {
    text("已将 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("的游戏模式切换为 ") with mochaPink
    text("<gamemode>") with mochaText
}

val COMMAND_GM_FAILED_SAME_GAMEMODE = component {
    text("你已经处于该模式了") with mochaMaroon
}

val COMMAND_GM_OTHER_FAILED_SAME_GAMEMODE = component {
    text("该玩家已经处于该模式了") with mochaMaroon
}
