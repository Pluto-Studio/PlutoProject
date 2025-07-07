package plutoproject.feature.paper.sit

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.title.*
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.*
import kotlin.time.Duration.Companion.seconds

val STAND_UP_TIP = component {
    text("按下 ") with mochaText
    keybind("key.sneak") with mochaLavender
    text(" 起身") with mochaText
}

val CAST_OFF_TIP = component {
    text("按下 ") with mochaText
    keybind("key.sneak") with mochaLavender
    text(" 摆脱其他玩家") with mochaText
}

val COMMAND_SIT = component {
    text("已在此处坐下") with mochaText
}

val COMMAND_SIT_FAILED_ALREADY_SITTING = component {
    text("你已经坐在一个位置上了") with mochaMaroon
}

val COMMAND_SIT_FAILED_TARGET_OCCUPIED = component {
    text("此处已有其他人坐下") with mochaMaroon
}

val COMMAND_SIT_FAILED_INVALID_TARGET = component {
    text("无法在此处坐下") with mochaMaroon
}

val COMMAND_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS = component {
    text("此处被遮挡，无法坐下") with mochaMaroon
}

val BLOCK_SIT_FAILED_TARGET_OCCUPIED_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此处已有其他人坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val BLOCK_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("此处被遮挡，无法坐下") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val PLAYER_SIT_FAILED_CARRIER_FEATURE_DISABLED = title {
    mainTitle {
        text(" ")
    }
    subTitle {
        text("对方已关闭玩家乘坐功能") with mochaMaroon
    }
    times {
        fadeIn(0.seconds)
        stay(1.seconds)
        fadeOut(0.seconds)
    }
}

val MENU_PLAYER_SIT_FEATURE_LOADING = component {
    text("正在加载...") with mochaSubtext0
}

val MENU_PLAYER_SIT_FEATURE_ENABLED = component {
    text("玩家乘坐 ") with mochaText
    text("开") with mochaGreen
}

val MENU_PLAYER_SIT_FEATURE_DISABLED = component {
    text("玩家乘坐 ") with mochaText
    text("关") with mochaMaroon
}

val MENU_PLAYER_SIT_FEATURE_DESC = listOf(
    component {
        text("通过 ") with mochaSubtext0
        keybind("key.use") with mochaLavender
        text(" 乘坐其他人") with mochaSubtext0
    },
    component {
        text("其他人也可以坐在你身上") with mochaSubtext0
    }
)

val MENU_PLAYER_SIT_FEATURE_OPERATION_ENABLE = component {
    text("左键 ") with mochaLavender
    text("开启功能") with mochaText
}

val MENU_PLAYER_SIT_FEATURE_OPERATION_DISABLE = component {
    text("左键 ") with mochaLavender
    text("关闭功能") with mochaText
}

val MENU_PLAYER_SIT_FEATURE_LORE_ENABLED = buildList {
    addAll(MENU_PLAYER_SIT_FEATURE_DESC)
    add(Component.empty())
    add(MENU_PLAYER_SIT_FEATURE_OPERATION_DISABLE)
}

val MENU_PLAYER_SIT_FEATURE_LORE_DISABLED = buildList {
    addAll(MENU_PLAYER_SIT_FEATURE_DESC)
    add(Component.empty())
    add(MENU_PLAYER_SIT_FEATURE_OPERATION_ENABLE)
}
