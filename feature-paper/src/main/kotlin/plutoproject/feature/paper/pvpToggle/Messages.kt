package plutoproject.feature.paper.pvpToggle

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import ink.pmc.advkt.title.*
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.*
import kotlin.time.Duration.Companion.seconds

val MENU_PVP_TOGGLE_FEATURE_LOADING = component {
    text("正在加载...") with mochaSubtext0
}

val MENU_PVP_TOGGLE_FEATURE_ENABLED = component {
    text("PvP ") with mochaText
    text("开") with mochaGreen
}

val MENU_PVP_TOGGLE_FEATURE_DISABLED = component {
    text("PvP ") with mochaText
    text("关") with mochaMaroon
}

val MENU_PVP_TOGGLE_FEATURE_DESC = listOf(
    component {
        text("双方都开启时才能互相伤害") with mochaSubtext0
    }
)

val MENU_PVP_TOGGLE_FEATURE_OPERATION_ENABLE = component {
    text("左键 ") with mochaLavender
    text("开启 PvP") with mochaText
}

val MENU_PVP_TOGGLE_FEATURE_OPERATION_DISABLE = component {
    text("左键 ") with mochaLavender
    text("关闭 PvP") with mochaText
}

val MENU_PVP_TOGGLE_FEATURE_LORE_ENABLED = buildList {
    addAll(MENU_PVP_TOGGLE_FEATURE_DESC)
    add(Component.empty())
    add(MENU_PVP_TOGGLE_FEATURE_OPERATION_DISABLE)
}

val MENU_PVP_TOGGLE_FEATURE_LORE_DISABLED = buildList {
    addAll(MENU_PVP_TOGGLE_FEATURE_DESC)
    add(Component.empty())
    add(MENU_PVP_TOGGLE_FEATURE_OPERATION_ENABLE)
}

val PVP_DISABLED_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("对方未开启 PvP") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}
