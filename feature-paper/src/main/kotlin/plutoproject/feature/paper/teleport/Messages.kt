package plutoproject.feature.paper.teleport

import ink.pmc.advkt.component.*
import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import ink.pmc.advkt.title.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.util.Ticks
import plutoproject.feature.paper.api.teleport.TeleportRequest
import plutoproject.framework.common.util.chat.palettes.*
import java.time.Duration

val TELEPORT_PREPARING_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Duration.ofMinutes(1))
        fadeOut(Duration.ZERO)
    }
    mainTitle {
        text("传送中") with mochaPink
    }
    subTitle {
        text("正在准备区块") with mochaText
    }
}

val TELEPORT_SUCCEED_MAINTITLE_CANDIDATES = arrayOf(
    "落地！",
    "到站了~",
    "请带好随身行李",
    "开门请当心",
    "下车请注意安全"
)

val teleportSucceedTitle = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text(TELEPORT_SUCCEED_MAINTITLE_CANDIDATES.random()) with mochaGreen
    }
    subTitle {
        text("已传送至目标位置") with mochaText
    }
}

val teleportSucceedTitleSafe = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text(TELEPORT_SUCCEED_MAINTITLE_CANDIDATES.random()) with mochaGreen
    }
    subTitle {
        text("已传送至附近的安全位置") with mochaText
    }
}

val TELEPORT_FAILED_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text("传送失败") with mochaMaroon
    }
    subTitle {
        text("无法找到安全位置") with mochaText
    }
}

val TELEPORT_FAILED_TIMEOUT_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text("传送失败") with mochaMaroon
    }
    subTitle {
        text("等待已超时") with mochaText
    }
}

val TELEPORT_FAILED_DENIED_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text("传送被阻止") with mochaMaroon
    }
    subTitle {
        text("<reason>") with mochaText
    }
}

val TELEPORT_DENIED_REASON_DEFAULT = component {
    text("请再试一次吧") with mochaText
}

val TELEPORT_FAILED_SOUND = sound {
    key(Key.key("block.amethyst_cluster.break"))
}

val TELEPORT_REQUEST_DENIED_SOUND = sound {
    key(Key.key("entity.villager.no"))
}

val TELEPORT_REQUEST_CANCELLED_SOUND = sound {
    key(Key.key("block.chain.hit"))
}

val TELEPORT_EXPIRE = component {
    text("该请求将在 ") with mochaSubtext0
    text("<expire> ") with mochaText
    text("后过期") with mochaSubtext0
}

val TELEPORT_REQUEST_AUTO_CANCEL = component {
    text("此前发送给 ") with mochaSubtext0
    text("<player> ") with mochaFlamingo
    text("的传送请求已自动取消") with mochaSubtext0
}

val TELEPORT_REQUEST_ACCEPTED_SOURCE = component {
    text("<player> ") with mochaYellow
    text("接受了你的传送请求") with mochaGreen
}

val TELEPORT_REQUEST_DENIED_SOURCE = component {
    text("<player> ") with mochaFlamingo
    text("拒绝了你的传送请求") with mochaMaroon
}

val TELEPORT_REQUEST_EXPIRED = component {
    text("来自 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("的传送请求已过期") with mochaYellow
}

val TELEPORT_REQUEST_EXPIRED_SOURCE = component {
    text("你向 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("发送的传送请求已过期") with mochaYellow
}

val TELEPORT_REQUEST_CANCELED = component {
    text("来自 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("的传送请求已被取消") with mochaYellow
}

val TELEPORT_REQUEST_CANCELED_OFFLINE = component {
    text("玩家 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("已离线，传送请求自动取消") with mochaYellow
}

val COMMAND_TPA_AFK = component {
    text("对方目前处于离开状态，可能无法及时查看请求") with mochaSubtext0
}

private val TELEPORT_REQUEST_OPERATION = component {
    text("[× 取消]") with mochaYellow with showText { text("点击以取消") with mochaYellow } with runCommand("/plutoproject:tpcancel")
}

val COMMAND_TPA_SUCCEED = component {
    text("已向 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("请求传送至其所在位置") with mochaPink
    newline()
    raw(TELEPORT_EXPIRE)
    newline()
    raw(TELEPORT_REQUEST_OPERATION)
}

val COMMAND_TPA_FAILED_SELF = component {
    text("你不能向自己发送传送请求") with mochaMaroon
}

val COMMAND_TPA_FAILED_TARGET_BUSY = component {
    text("对方仍有未处理的传送请求，请稍后再试") with mochaMaroon
}

val COMMAND_TPA_FAILED_NOT_ALLOWED_GO = component {
    text("玩家 ") with mochaMaroon
    text("<player> ") with mochaFlamingo
    text("所在的世界不允许被传送") with mochaMaroon
}

val COMMAND_TPA_FAILED_NOT_ALLOWED_COME = component {
    text("你所在的世界不允许被传送") with mochaMaroon
}

val COMMAND_TPAHERE_SUCCEED = component {
    text("已向 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("请求传送到你所在的位置") with mochaPink
    newline()
    raw(TELEPORT_EXPIRE)
    newline()
    raw(TELEPORT_REQUEST_OPERATION)
}

val TELEPORT_TPA_RECEIVED = component {
    text("<player> ") with mochaFlamingo
    text("请求传送到") with mochaPink
    text("你所在的位置") with mochaPink with underlined()
}

val TELEPORT_TPAHERE_RECEIVED = component {
    text("<player> ") with mochaFlamingo
    text("请求将你传送至") with mochaPink
    text("其所在的位置") with mochaPink with underlined()
}

val COMMAND_TPACCEPT_SUCCEED = component {
    text("已接受来自 ") with mochaGreen
    text("<player> ") with mochaYellow
    text("的传送请求") with mochaGreen
}

val COMMAND_TPDENY_SUCCEED = component {
    text("已拒绝来自 ") with mochaMaroon
    text("<player> ") with mochaFlamingo
    text("的传送请求") with mochaMaroon
}

val COMMAND_TPACCEPT_FAILED_NO_PENDING = component {
    text("你暂时没有未接受的传送请求") with mochaMaroon
}

val COMMAND_TPACCEPT_FAILED_NO_REQUEST = component {
    text("你没有来自 ") with mochaMaroon
    text("<player> ") with mochaFlamingo
    text("的未接受请求") with mochaMaroon
}

val COMMAND_TPACCEPT_FAILED_NO_REQUEST_ID = component {
    text("请求已过期或不存在") with mochaMaroon
    newline()
    text("如果你认为这是一个错误，请上报给管理组") with mochaSubtext0
}

val COMMAND_TPCANCEL_SUCCEED = component {
    text("已取消发送给 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("的传送请求") with mochaYellow
}

val COMMAND_TPCANCEL_SUCCEED_OTHER = component {
    text("已取消 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("发送给 ") with mochaYellow
    text("<dest> ") with mochaFlamingo
    text("的传送请求") with mochaYellow
}

val COMMAND_TPCANCEL_OTHER_NOTIFY = component {
    text("管理员取消了你发送给 ") with mochaYellow
    text("<player> ") with mochaFlamingo
    text("的传送请求") with mochaYellow
}

val COMMAND_TPCANCEL_NO_REQUEST = component {
    text("你没有未完成的传送请求") with mochaMaroon
}

val COMMAND_TPCANCEL_NO_REQUEST_OTHER = component {
    text("<player> ") with mochaFlamingo
    text("没有未完成的传送请求") with mochaMaroon
}

fun getTeleportOperationMessage(request: TeleportRequest) = component {
    text("[✔ 接受] ") with mochaGreen with showText { text("点击以接受") with mochaGreen } with runCommand("/plutoproject:tpaccept ${request.id}")
    text("[❌ 拒绝] ") with mochaMaroon with showText { text("点击以拒绝") with mochaMaroon } with runCommand("/plutoproject:tpdeny ${request.id}")
}
