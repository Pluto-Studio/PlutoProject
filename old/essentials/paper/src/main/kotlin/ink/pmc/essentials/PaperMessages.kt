package ink.pmc.essentials

import ink.pmc.advkt.component.*
import ink.pmc.advkt.component.replace
import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.sound
import ink.pmc.advkt.title.*
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.api.teleport.random.RandomTeleportManager
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.chat.*
import ink.pmc.framework.world.aliasOrName
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.Ticks
import org.bukkit.Location
import java.time.Duration
import java.util.*
import kotlin.time.toKotlinDuration

val GM_SURVIVAL = Component.text("生存模式")

val GM_CREATIVE = Component.text("创造模式")

val GM_ADVENTURE = Component.text("冒险模式")

val GM_SPECTATOR = Component.text("旁观模式")

val COMMAND_GM_SUCCCEED = component {
    text("已将游戏模式切换为 ") with mochaPink
    text("<gamemode>") with mochaText
}

val COMMAND_GM_OTHER_SUCCCEED = component {
    text("已将 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("的游戏模式切换为 ") with mochaPink
    text("<gamemode>") with mochaText
}

val COMMAND_GM_FAILED = component {
    text("你已经处于该模式了") with mochaMaroon
}

val COMMAND_GM_FAILED_OTHER = component {
    text("该玩家已经处于该模式了") with mochaMaroon
}

val COMMAND_ALIGN_SUCCEED = component {
    text("已对齐你的视角和位置") with mochaPink
}

val COMMAND_ALIGN_POS_SUCCEED = component {
    text("已对齐你的位置") with mochaPink
}

val COMMAND_ALIGN_VIEW_SUCCEED = component {
    text("已对齐你的视角") with mochaPink
}

val COMMAND_HAT_SUCCEED = component {
    text("享受你的新帽子吧！") with mochaPink
}

val COMMAND_HAT_FAILED_EMPTY_HAND = component {
    text("你的手上似乎空空如也") with mochaMaroon
    newline()
    text("将你想要戴在头上的物品放入手中，然后再试一次吧") with mochaSubtext0
}

val COMMAND_HAT_SUCCEED_OTHER = component {
    text("已将你手中的物品戴在 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("的头上") with mochaPink
}

val COMMAND_HAT_FAILED_EXISTED_OTHER = component {
    text("该玩家的头上似乎已经有物品了") with mochaMaroon
}

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

val TELEPORT_PREPARING_SOUND = sound {
    key(Key.key("block.amethyst_cluster.hit"))
}

val TELEPORT_SUCCEED_MAINTITLE_ARRAY = arrayOf(
    "落地！",
    "到站了~",
    "请带好随身行李",
    "开门请当心",
    "下车请注意安全"
)

val TELEPORT_SUCCEED_MAINTITLE
    get() = TELEPORT_SUCCEED_MAINTITLE_ARRAY.random()

val TELEPORT_SUCCEED_TITLE
    get() = title {
        times {
            fadeIn(Ticks.duration(5))
            stay(Ticks.duration(35))
            fadeOut(Ticks.duration(20))
        }
        mainTitle {
            text(TELEPORT_SUCCEED_MAINTITLE) with mochaGreen
        }
        subTitle {
            text("已传送至目标位置") with mochaText
        }
    }

val TELEPORT_SUCCEED_TITLE_SAFE
    get() = title {
        times {
            fadeIn(Ticks.duration(5))
            stay(Ticks.duration(35))
            fadeOut(Ticks.duration(20))
        }
        mainTitle {
            text(TELEPORT_SUCCEED_MAINTITLE) with mochaGreen
        }
        subTitle {
            text("已传送至附近的安全位置") with mochaText
        }
    }

val TELEPORT_SUCCEED_SOUND = sound {
    key(Key.key("entity.enderman.teleport"))
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

@Suppress("FunctionName")
fun TELEPORT_FAILED_DEINED_TITLE(reason: Component) = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Ticks.duration(35))
        fadeOut(Ticks.duration(20))
    }
    mainTitle {
        text("传送被阻止") with mochaMaroon
    }
    subTitle {
        raw(reason) with mochaText
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

val TELEPORT_REQUEST_RECEIVED_SOUND = sound {
    key(Key.key("block.decorated_pot.insert"))
}

private val TELEPORT_REQUEST_OPERATION = component {
    text("[× 取消]") with mochaYellow with showText { text("点击以取消") with mochaYellow } with runCommand("/plutoproject_essentials:tpcancel")
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

@Suppress("FunctionName")
fun TELEPORT_OPERATION(id: UUID) = component {
    text("[✔ 接受] ") with mochaGreen with showText { text("点击以接受") with mochaGreen } with runCommand("/plutoproject_essentials:tpaccept $id")
    text("[❌ 拒绝] ") with mochaMaroon with showText { text("点击以拒绝") with mochaMaroon } with runCommand("/plutoproject_essentials:tpdeny $id")
}

val COMMAND_ETP_SUCCEED = component {
    text("已传送至 ") with mochaPink
    text("<location> ") with mochaText
}

val COMMAND_ETP_SUCCEED_OTHER = component {
    text("已将玩家 ") with mochaPink
    text("<player> ") with mochaFlamingo
    text("传送至 ") with mochaPink
    text("<location>") with mochaText
}

val RANDOM_TELEPORT_SUCCEED = component {
    text("已将你传送到 ") with mochaPink
    text("<location>") with mochaText
    newline()
    text("本次传送尝试 ") with mochaSubtext0
    text("<attempts> ") with mochaText
    text("次，耗时 ") with mochaSubtext0
    text("<lastLookupTime>") with mochaText
}

val RANDOM_TELEPORT_SUCCEED_COST = component {
    raw(RANDOM_TELEPORT_SUCCEED)
    text("，花费 ") with mochaSubtext0
    text("<amount><symbol>") with mochaText
}

val RANDOM_TELEPORT_SEARCHING_TITLE = title {
    times {
        fadeIn(Ticks.duration(5))
        stay(Duration.ofMinutes(1))
        fadeOut(Duration.ZERO)
    }
    mainTitle {
        text("传送中") with mochaPink
    }
    subTitle {
        text("正在搜寻目的地") with mochaText
    }
}

val RANDOM_TELEPORT_SEARCHING_SOUND = sound {
    key(Key.key("entity.tnt.primed"))
}

val RANDOM_TELEPORT_SEARCHING_FAILED = component {
    text("这次运气似乎不太好...") with mochaYellow
    newline()
    text("如果此问题总是发生，请报告给管理组") with mochaSubtext0
}

val RANDOM_TELEPORT_FAILED_IN_PROGRESS = component {
    text("你已有一个正在处理的随机传送操作，请不要着急") with mochaYellow
}

val COMMAND_RTP_NOT_ENABLED = component {
    text("该世界未启用随机传送") with mochaMaroon
}

val COMMAND_RTP_COOLDOWN = component {
    text("传送冷却中...") with mochaSubtext0
    newline()
    text("请 ") with mochaSubtext0
    text("<time> ") with mochaText
    text("后再试") with mochaSubtext0
}

val COMMAND_ESS_RTP
    get() = component {
        val manager = RandomTeleportManager

        text("随机传送状态：") with mochaFlamingo
        newline()

        text("  - ") with mochaSubtext0
        text("已处理刻数：") with mochaText
        text("${manager.tickCount}") with mochaLavender
        newline()

        text("  - ") with mochaSubtext0
        text("上次刻处理用时：") with mochaText
        raw(DURATION(Duration.ofMillis(manager.lastTickTime).toKotlinDuration())) with mochaLavender

        newline()
        empty()
        newline()

        text("世界缓存数：") with mochaFlamingo
        newline()

        val worlds = manager.enabledWorlds
        worlds.forEach {
            val caches = manager.getCaches(it).size
            text("  - ") with mochaSubtext0
            text("${it.name}：") with mochaText
            text(caches) with mochaLavender
            if (worlds.last() != it) {
                newline()
            }
        }
    }

val COMMAND_ESS_RTP_PERF_START = component {
    text("已开启性能测试，再次使用本指令以关闭") with mochaPink
}

val COMMAND_ESS_RTP_PERF_END = component {
    text("已关闭性能测试") with mochaPink
}

val COMMAND_SETHOME_FAILED_REACH_LIMIT
    get() = component {
        text("你当前设置的家数量已经到达上限，请删除一些再试") with mochaMaroon
        newline()
        text("当前家上限数量为 ") with mochaSubtext0
        text("${HomeManager.maxHomes} ") with mochaText
        text("个") with mochaSubtext0
    }

val COMMAND_SETHOME_FAILED_EXISTED = component {
    text("你已经有一个名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家了") with mochaMaroon
    newline()
    text("请删除或更换一个名字后再试") with mochaSubtext0
}

val COMMAND_SETHOME_FAILED_NOT_VALID = component {
    text("家的名字只可以包含字母、数字、下划线") with mochaMaroon
    newline()
    text("不可以使用中文、空格等字符") with mochaSubtext0
}

val COMMAND_SETHOME_FAILED_LENGTN_LIMIT
    get() = component {
        text("家的名字最多只能使用 ") with mochaMaroon
        text("${HomeManager.nameLengthLimit} ") with mochaText
        text("个字符") with mochaMaroon
        newline()
        text("请缩短一些后再试") with mochaSubtext0
    }

val COMMAND_SETHOME_SUCCEED = component {
    text("已设置名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_HOME_NOT_EXISTED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家不存在") with mochaMaroon
}

val COMMAND_EDITHOME_ALREADY_PREFERRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经是首选了") with mochaMaroon
}

val COMMAND_EDITHOME_PREFER_SUCCEED = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家设为首选") with mochaPink
}

val COMMAND_EDITHOME_ALREADY_STARRED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的家已经收藏过了") with mochaMaroon
}

val COMMAND_EDITHOME_STAR_SUCCEED = component {
    text("已将名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家收藏") with mochaPink
}

val COMMAND_EDITHOME_RENAME_SUCCEED = component {
    text("已将该家更名为 ") with mochaPink
    text("<new_name>") with mochaText
}

val COMMAND_EDITHOME_MOVE_SUCCEED = component {
    text("已将家 ") with mochaPink
    text("<name> ") with mochaText
    text("迁移到你所在的位置") with mochaPink
}

val COMMAND_HOME_NOT_EXISTED_UUID = component {
    text("无法通过指定的 ID 找到对应的家") with mochaMaroon
}

val COMMAND_HOME_SUCCEED = component {
    text("已传送到名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_DELHOME_SUCCEED = component {
    text("已删除名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的家") with mochaPink
}

val COMMAND_WARP_NOT_EXISTED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的地标不存在") with mochaMaroon
}

val COMMAND_PREFERRED_SPAWN_WARP_IS_NOT_SPAWN = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的地标不是一个出生点") with mochaMaroon
}

val COMMAND_PREFERRED_SPAWN_SUCCEED = component {
    text("已将你的默认出生点设为 ") with mochaPink
    text("<name>") with mochaText
}

val COMMAND_PREFERRED_SPAWN_FAILED_ALREADY = component {
    text("你的默认出生点已是 ") with mochaMaroon
    text("<name>") with mochaText
}

val COMMAND_WARP_FAILED_NOT_EXISTED_UUID = component {
    text("无法通过指定的 ID 找到对应的地标") with mochaMaroon
}

val COMMAND_WARP_SUCCEED = component {
    text("已传送到名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的地标") with mochaPink
}

val COMMAND_WARP_SUCCEED_ALIAS = component {
    text("已传送到名为 ") with mochaPink
    text("<alias> ") with mochaText
    text("(<name>) ") with mochaSubtext0
    text("的地标") with mochaPink
}

val COMMAND_SETWARP_FAILED_EXISTED = component {
    text("名为 ") with mochaMaroon
    text("<name> ") with mochaText
    text("的地标已存在") with mochaMaroon
}

val COMMAND_SETWARP_FAILED_NOT_VALID = component {
    text("地标的名字只可以包含字母、数字、下划线") with mochaMaroon
    newline()
    text("不可以使用中文、空格等字符") with mochaSubtext0
}

val COMMAND_SETWARP_FAILED_LENGTN_LIMIT
    get() = component {
        text("地标的名字最多只能使用 ") with mochaMaroon
        text("${WarpManager.nameLengthLimit} ") with mochaText
        text("个字符") with mochaMaroon
        newline()
        text("请缩短一些后再试") with mochaSubtext0
    }

val COMMAND_SETWARP_SUCCEED = component {
    text("已设置名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的地标") with mochaPink
}

val COMMAND_SETWARP_SUCCEED_ALIAS = component {
    text("已设置名为 ") with mochaPink
    text("<alias> ") with mochaText
    text("(<name>) ") with mochaSubtext0
    text("的地标") with mochaPink
}

val COMMAND_DELWARP_SUCCEED = component {
    text("已删除名为 ") with mochaPink
    text("<name> ") with mochaText
    text("的地标") with mochaPink
}

val COMMAND_DELWARP_SUCCEED_ALIAS = component {
    text("已删除名为 ") with mochaPink
    text("<alias> ") with mochaText
    text("(<name>) ") with mochaSubtext0
    text("的地标") with mochaPink
}

val COMMAND_BACK_FAILED_NO_LOC = component {
    text("还没有记录到你先前的位置") with mochaMaroon
    newline()
    text("进行一段时间游玩后位置才会被记录") with mochaSubtext0
}

val COMMAND_BACK_SUCCEED = component {
    text("已回到你先前的位置") with mochaPink
}

val AFK_START_ANNOUNCE = component {
    text("* <player> 暂时离开了") with mochaSubtext0
}

val AFK_END_ANNOUNCE = component {
    text("* <player> 回来了") with mochaSubtext0
}

val COMMAND_IF_FAILED_NO_FRAME = component {
    text("你需要对着一个物品展示框才可以这么做") with mochaMaroon
}

val COMMAND_IF_INV_ON_SUCCEED = component {
    text("已将你面前的展示框隐藏") with mochaPink
}

val COMMAND_IF_INV_OFF_SUCCEED = component {
    text("已将你面前的展示框显现") with mochaPink
}

val COMMAND_IF_PROTECT_ON_SUCCEED = component {
    text("已将你面前的展示框保护") with mochaPink
}

val COMMAND_IF_PROTECT_OFF_SUCCEED = component {
    text("已将你面前的展示框取消保护") with mochaPink
}

val IF_PROTECTED_ACTION = component {
    text("此展示框已被 ") with mochaSubtext0
    text("<player> ") with mochaText
    text("保护") with mochaSubtext0
}

const val IF_PROTECT_UNKNOWN_PLAYER = "未知玩家"

val IF_UNFINISHED_BOOK = Component.text("未完成的书")

val IF_UNFINISHED_BOOK_AUTHOR = Component.text("未知作者")

val COMMAND_LECT_FAILED_NO_LECTERN = component {
    text("你需要对着一个讲台才可以这么做") with mochaMaroon
}

val COMMAND_LECT_PROTECT_ON_SUCCEED = component {
    text("已将你面前的讲台保护") with mochaPink
}

val COMMAND_LECT_PROTECT_OFF_SUCCEED = component {
    text("已将你面前的讲台取消保护") with mochaPink
}

val LECT_PROTECTED_ACTION = component {
    text("此讲台已被 ") with mochaSubtext0
    text("<player> ") with mochaText
    text("保护") with mochaSubtext0
}

const val DEFAULT_ECONOMY_SYMBOL = "\uD83C\uDF1F"

val RANDOM_TELEPORT_BALANCE_NOT_ENOUGH = component {
    text("货币不足，进行随机传送需要 ") with mochaMaroon
    text("<amount> <symbol>") with mochaText
    newline()
    text("你当前拥有 ") with mochaSubtext0
    text("<balance> <symbol>") with mochaText
}

val UI_VIEWER_LOADING_TITLE = component {
    text("正在加载数据")
}

val UI_VIEWER_LOADING = component {
    text("正在加载数据...") with mochaSubtext0 without italic()
}

val UI_VIEWER_EMPTY = component {
    text("这里空空如也") with mochaText without italic()
}

private val UI_HOME_EMPTY_PROMPT = component {
    text("点击下方按钮或 ") with mochaSubtext0 without italic()
    text("/sethome ") with mochaLavender without italic()
    text("以留下你的足迹") with mochaSubtext0 without italic()
}

val UI_HOME_EMPTY_LORE = listOf(
    UI_HOME_EMPTY_PROMPT
)

val UI_HOME_EMPTY_LORE_OTHER = listOf(
    component { text("该玩家未设置家") with mochaSubtext0 without italic() }
)

val UI_HOME_TITLE = component {
    text("<player> 的家")
}

val UI_HOME_TITLE_SELF = component {
    text("家")
}

val UI_HOME_ITEM_NAME = component {
    text("<name>") with mochaYellow without italic()
}

private val UI_HOME_PREFERRED = component {
    text("√ 首选的家") with mochaGreen without italic()
}

private val UI_HOME_STARRED = component {
    text("✨ 收藏的家") with mochaYellow without italic()
}

private val UI_HOME_ITEM_LORE_LOC = component {
    text("<world> <x>, <y>, <z>") with mochaSubtext0 without italic()
}

@Suppress("FunctionName")
fun UI_HOME_ITEM_LORE(home: Home): List<Component> {
    val loc = home.location
    return mutableListOf<Component>().apply {
        add(component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        })
        if (home.isPreferred) add(UI_HOME_PREFERRED)
        if (home.isStarred) add(UI_HOME_STARRED)
        add(Component.empty())
        add(component {
            text("左键 ") with mochaLavender without italic()
            text("传送到该位置") with mochaText without italic()
        })
        add(component {
            text("右键 ") with mochaLavender without italic()
            text("编辑家") with mochaText without italic()
        })
    }
}

val VIEWER_PAGING = component {
    text("页 <curr>/<total>") with mochaText without italic()
}

val VIEWING_PAGE_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("下一页") with mochaText without italic()
    },
    component {
        text("右键 ") with mochaLavender without italic()
        text("上一页") with mochaText without italic()
    }
)

val VIEWER_PAGING_SOUND = sound {
    key(Key.key("item.book.page_turn"))
}

val UI_WARP_TITLE = component {
    text("地标")
}

val UI_WARP_ITEM_NAME = component {
    text("<name>") with mochaPink without italic()
}

val UI_WARP_ITEM_NAME_ALIAS = component {
    text("<alias> ") with mochaPink without italic()
    text("(<name>)") with mochaSubtext0 without italic()
}

private val UI_WARP_ITEM_LORE_LOC = component {
    text("<world> <x>, <y>, <z>") with mochaSubtext0 without italic()
}

@Suppress("FunctionName")
fun UI_WARP_ITEM_LORE(warp: Warp): List<Component> {
    val loc = warp.location
    return listOf(
        component {
            raw(
                UI_WARP_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender without italic()
            text("传送到该位置") with mochaText without italic()
        },
    )
}

val UI_WARP_EMPTY_LORE = listOf(
    component { text("服务器未设置地标") with mochaSubtext0 without italic() }
)

val UI_HOME_EDITOR_TITLE = component {
    text("编辑 <name>")
}

val UI_HOME_RENAME = component {
    text("重命名") with mochaText without italic()
}

val UI_HOME_RENAME_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("重命名该家") with mochaText without italic()
    }
)

val UI_HOME_CHANGE_LOCATION = component {
    text("迁移") with mochaText without italic()
}

val UI_HOME_CHANGE_LOCATION_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家迁移到你所在的位置") with mochaText without italic()
    }
)

val UI_HOME_DELETE = component {
    text("删除") with mochaText without italic()
}

val UI_HOME_DELETE_LORE = listOf(
    component { text("该操作不可撤销") with mochaRed without italic() },
    Component.empty(),
    component {
        text("Shift + 左键 ") with mochaLavender without italic()
        text("删除该家") with mochaText without italic()
    }
)

val UI_HOME_EDIT_SUCCEED = component {
    text("√ 已保存") with mochaGreen without italic()
}

val UI_HOME_PREFER = component {
    text("设为首选") with mochaText without italic()
}

val UI_HOME_PREFER_LORE = listOf(
    component { text("设为首选后，") with mochaSubtext0 without italic() },
    component {
        text("使用 ") with mochaSubtext0 without italic()
        text("/home ") with mochaLavender without italic()
        text("或「手账」将默认传送该家") with mochaSubtext0 without italic()
    },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家设为首选") with mochaText without italic()
    }
)

val UI_HOME_PREFER_UNSET = component {
    text("取消首选") with mochaText without italic()
}


val UI_HOME_PREFER_UNSET_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家取消首选") with mochaText without italic()
    }
)

val UI_HOME_STAR = component {
    text("收藏") with mochaText without italic()
}

val UI_HOME_STAR_LORE = listOf(
    component { text("收藏后，该家将靠前显示") with mochaSubtext0 without italic() },
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家收藏") with mochaText without italic()
    }
)

val UI_HOME_STAR_UNSET = component {
    text("取消收藏") with mochaText without italic()
}

val UI_HOME_STAR_UNSET_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("将该家取消收藏") with mochaText without italic()
    }
)

val UI_HOME_EDIT_SUCCEED_SOUND = sound {
    key(Key.key("block.note_block.bell"))
}

val UI_HOME_EDITOR_REMOVE_SOUND = sound {
    key(Key.key("block.decorated_pot.break"))
}

val UI_HOME_EDITOR_RENAME_TITLE = component {
    text("重命名 <name>")
}

val UI_HOME_EDITOR_RENAME_EXIT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("退出编辑") with mochaText without italic()
    }
)

@Suppress("FunctionName")
fun UI_HOME_EDITOR_RENAME_SAVE_EDITING(home: Home): List<Component> {
    val loc = home.location
    return listOf(
        component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender without italic()
            text("保存并退出") with mochaText without italic()
        }
    )
}

val UI_HOME_EDITOR_RENAME_SAVE_INVALID_LORE = listOf(
    Component.empty(),
    component { text("仅可使用字母、数字、下划线") with mochaMaroon without italic() }
)

val UI_HOME_EDITOR_RENAME_SAVE_TOO_LONG
    get() = listOf(
        Component.empty(),
        component {
            text("名称过长，最多使用 ") with mochaMaroon without italic()
            text("${HomeManager.nameLengthLimit} ") with mochaText without italic()
            text("个字符") with mochaMaroon without italic()
        }
    )

val UI_HOME_EDITOR_RENAME_SAVE_EXISTED = listOf(
    Component.empty(),
    component {
        component { text("已存在同名的家") with mochaMaroon without italic() }
    }
)

val UI_HOME_EDITOR_RENAME_SAVED = listOf(
    Component.empty(),
    UI_HOME_EDIT_SUCCEED
)

val UI_HOME_EDITOR_RENAME_INVALID_SOUND = sound {
    key(Key.key("block.note_block.didgeridoo"))
}

val UI_HOME_CREATOR_TITLE = component {
    text("创建家")
}

val UI_HOME_CREATOR_LEFT_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("返回上一页") with mochaText without italic()
    }
)

const val UI_HOME_CREATOR_INPUT = "输入名称..."

@Suppress("FunctionName")
fun UI_HOME_CREATOR_OUTPUT_LORE(loc: Location): List<Component> {
    return listOf(
        component {
            raw(
                UI_HOME_ITEM_LORE_LOC
                    .replace("<world>", loc.world.aliasOrName)
                    .replace("<x>", "${loc.blockX}")
                    .replace("<y>", "${loc.blockY}")
                    .replace("<z>", "${loc.blockZ}")
            )
        },
        Component.empty(),
        component {
            text("左键 ") with mochaLavender without italic()
            text("创建家") with mochaText without italic()
        }
    )
}

val UI_HOME_VIEWER_CREATE = component {
    text("创建家") with mochaText without italic()
}

val UI_HOME_VIEWER_CREATE_LORE = listOf(
    Component.empty(),
    component {
        text("左键 ") with mochaLavender without italic()
        text("在当前位置创建家") with mochaText without italic()
    }
)

val COMMAND_SPAWN_FAILED_NOT_SET = component {
    text("服务器未设置出生点位置") with mochaMaroon
}