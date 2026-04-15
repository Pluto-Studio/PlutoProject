package plutoproject.feature.gallery.adapter.paper

import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import plutoproject.framework.common.util.chat.palettes.*

const val IMAGE_PLACEHOLDER_NAME = "<name>"
const val IMAGE_PLACEHOLDER_WIDTH = "<width>"
const val IMAGE_PLACEHOLDER_HEIGHT = "<height>"
const val IMAGE_PLACEHOLDER_CREATOR = "<creator>"
const val IMAGE_PLACEHOLDER_TIME = "<time>"
const val IMAGE_PLACEHOLDER_TYPE = "<type>"
const val IMAGE_PLACEHOLDER_MAX_MAP_BLOCKS = "<maxMapBlocks>"
const val IMAGE_PLACEHOLDER_MAX_NAME_LENGTH = "<maxNameLength>"
const val IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER = "<maxImagesPerPlayer>"
const val IMAGE_PLACEHOLDER_EXPIRE = "<expire>"
const val IMAGE_PLACEHOLDER_FILE_NAME = "<fileName>"
const val IMAGE_PLACEHOLDER_FRAME_COUNT = "<frameCount>"
const val IMAGE_PLACEHOLDER_IMAGE_WIDTH = "<imageWidth>"
const val IMAGE_PLACEHOLDER_IMAGE_HEIGHT = "<imageHeight>"

// Migrator
const val IMAGE_PLACEHOLDER_PROCESSED = "<processed>"
const val IMAGE_PLACEHOLDER_SUCCESS = "<success>"
const val IMAGE_PLACEHOLDER_FAILED = "<failed>"
const val IMAGE_PLACEHOLDER_IMAGE_ID = "<imageId>"
const val IMAGE_PLACEHOLDER_REASON = "<reason>"

val IMAGE_LIST_MENU_BUTTON = component {
    text("地图画") with mochaYellow
}

val IMAGE_LIST_MENU_BUTTON_LORE_DESC = component {
    text("查看与管理你的地图画") with mochaSubtext0
}

val IMAGE_LIST_MENU_BUTTON_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("打开地图画列表") with mochaText
}

val IMAGE_LIST_TITLE = component {
    text("地图画")
}

val IMAGE_LIST_CREATE = component {
    text("创建地图画") with mochaText
}

val IMAGE_LIST_CREATE_LORE_OPERATION = component {
    text("左键 ") with mochaLavender
    text("创建地图画") with mochaText
}

val IMAGE_LIST_CREATE_LORE_UNAVAILABLE = component {
    text("填写名称与尺寸后开始上传") with mochaSubtext0
}

val IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD = component {
    text("你已有未完成的创建请求") with mochaMaroon
}

val IMAGE_LIST_CREATE_LORE_UNFINISHED_UPLOAD_HINT = component {
    text("请先完成或取消当前请求") with mochaSubtext0
}

val IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES = component {
    text("你拥有的地图画已达到上限") with mochaMaroon
}

val IMAGE_LIST_CREATE_LORE_TOO_MANY_IMAGES_HINT = component {
    text("当前最多可拥有 ") with mochaSubtext0
    text("$IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER ") with mochaText
    text("幅") with mochaSubtext0
}

val IMAGE_LIST_ENTRY_NAME = component {
    text(IMAGE_PLACEHOLDER_NAME) with mochaYellow
}

val IMAGE_LIST_ENTRY_LORE_TYPE = component {
    text("类型: ") with mochaSubtext0
    text(IMAGE_PLACEHOLDER_TYPE) with mochaText
}

val IMAGE_LIST_ENTRY_LORE_SIZE = component {
    text("尺寸: ") with mochaSubtext0
    text("$IMAGE_PLACEHOLDER_WIDTH × $IMAGE_PLACEHOLDER_HEIGHT") with mochaText
}

val IMAGE_LIST_ENTRY_LORE_OPERATION = component {
    text("右键 ") with mochaLavender
    text("删除该地图画") with mochaText
}

const val IMAGE_LIST_ENTRY_TYPE_STATIC = "静态"
const val IMAGE_LIST_ENTRY_TYPE_ANIMATED = "动态"

val IMAGE_CREATE_TITLE = component {
    text("创建地图画") with mochaText
}

val IMAGE_CREATE_DESCRIPTION = component {
    text("填写名称与尺寸后，将向你发送上传链接") with mochaSubtext0
}

val IMAGE_CREATE_NAME_INPUT_LABEL = component {
    text("名称") with mochaText
}

val IMAGE_CREATE_WIDTH_INPUT_LABEL = component {
    text("宽方块数")
}

val IMAGE_CREATE_HEIGHT_INPUT_LABEL = component {
    text("高方块数")
}

val IMAGE_CREATE_DITHER_INPUT_LABEL = component {
    text("抖动")
}

val IMAGE_CREATE_DITHER_OPTION_DEFAULT_DISPLAY = Component.text("默认")

val IMAGE_CREATE_DITHER_OPTION_NONE_DISPLAY = Component.text("无")

val IMAGE_CREATE_DITHER_OPTION_FLOYD_STEINBERG_DISPLAY = Component.text("Floyd-Steinberg 误差扩散")

val IMAGE_CREATE_DITHER_OPTION_ORDERED_BAYER_DISPLAY = Component.text("有序抖动（Bayer）")

val IMAGE_CREATE_DITHER_INPUT_LABEL_HOVER = component {
    text("控制图像颜色如何转换到地图颜色。") with mochaText
    newline()
    text("默认：使用系统默认策略。") with mochaSubtext0
    newline()
    text("无：直接转换为最接近的颜色，可能出现明显色带。") with mochaSubtext0
    newline()
    text("Floyd-Steinberg 误差扩散：通过误差扩散平滑颜色过渡，通常效果最佳。") with mochaSubtext0
    newline()
    text("有序抖动（Bayer）：使用 Bayer 矩阵进行有序抖动，产生规则纹理以模拟中间色。") with mochaSubtext0
}

val IMAGE_CREATE_DITHER_ABOUT = component {
    text("关于「抖动」") with mochaText with showText {
        raw(IMAGE_CREATE_DITHER_INPUT_LABEL_HOVER)
    }
}

val IMAGE_CREATE_FILL_INPUT_LABEL = component {
    text("填充")
}

val IMAGE_CREATE_FILL_OPTION_CONTAIN_DISPLAY = Component.text("完整显示")

val IMAGE_CREATE_FILL_OPTION_COVER_DISPLAY = Component.text("缩放裁剪")

val IMAGE_CREATE_FILL_OPTION_STRETCH_DISPLAY = Component.text("拉伸填充")

val IMAGE_CREATE_FILL_INPUT_LABEL_HOVER = component {
    text("控制图像如何填充到地图画区域。") with mochaText
    newline()
    text("完整显示：按比例缩放图像，使其完整显示在目标区域内，可能出现留边。") with mochaSubtext0
    newline()
    text("缩放裁剪：按比例缩放图像以填满目标区域，超出部分会被裁剪。") with mochaSubtext0
    newline()
    text("拉伸填充：拉伸图像以填满目标区域，不保持比例，可能导致变形。") with mochaSubtext0
}

val IMAGE_CREATE_FILL_ABOUT = component {
    text("关于「填充」") with mochaText with showText {
        raw(IMAGE_CREATE_FILL_INPUT_LABEL_HOVER)
    }
}

val IMAGE_CREATE_SUBMIT = component {
    text("提交")
}

val IMAGE_CREATE_CANCEL = component {
    text("取消")
}

val IMAGE_CREATE_FAILED_EMPTY_NAME = component {
    text("请输入名称") with mochaMaroon
}

val IMAGE_CREATE_FAILED_TOO_LONG = component {
    text("名称过长，最多使用 ") with mochaMaroon
    text("$IMAGE_PLACEHOLDER_MAX_NAME_LENGTH ") with mochaText
    text("个字符") with mochaMaroon
}

val IMAGE_CREATE_FAILED_TOO_MANY_MAP_BLOCKS = component {
    text("尺寸过大") with mochaMaroon
    newline()
    text("宽方块数 × 高方块数不能超过 ") with mochaText
    text(IMAGE_PLACEHOLDER_MAX_MAP_BLOCKS) with mochaLavender
}

val IMAGE_CREATE_FAILED_UNFINISHED_UPLOAD = component {
    text("你已有未完成的地图画创建请求") with mochaMaroon
    newline()
    text("请先完成或取消当前请求") with mochaText
}

val IMAGE_CREATE_FAILED_TOO_MANY_IMAGES = component {
    text("你拥有的地图画数量已达到上限") with mochaMaroon
    newline()
    text("当前最多可拥有 ") with mochaText
    text("$IMAGE_PLACEHOLDER_MAX_IMAGES_PER_PLAYER ") with mochaLavender
    text("幅") with mochaText
}

val IMAGE_CREATE_SESSION_CREATED_PREFIX = component {
    text("已创建地图画上传请求") with mochaText
}

val IMAGE_CREATE_SESSION_CREATED_OPEN = component {
    text("[点击打开]") with mochaLavender with underlined()
}

val IMAGE_CREATE_SESSION_CREATED_CANCEL = component {
    text("[取消创建]") with mochaLavender with showText {
        text("点击取消当前创建请求") with mochaLavender
    } with runCommand("/gallery cancel-upload")
}

val IMAGE_CREATE_SESSION_CREATED_SUFFIX = component {
    text("该请求将在 ") with mochaText
    text("$IMAGE_PLACEHOLDER_EXPIRE ") with mochaLavender
    text("后过期") with mochaText
}

val IMAGE_CREATE_SESSION_PROCESSING = component {
    text("已收到上传文件，正在验证内容") with mochaText
}

val IMAGE_CREATE_SESSION_CREATING = component {
    text("上传成功，正在创建地图画") with mochaText
}

val IMAGE_CREATE_SESSION_EXPIRED = component {
    text("地图画创建请求已过期") with mochaMaroon
}

val IMAGE_CREATE_SESSION_CANCELLED = component {
    text("地图画创建请求已取消") with mochaYellow
}

val IMAGE_CREATE_SESSION_FAILED = component {
    text("地图画创建请求失败，请稍后再试") with mochaMaroon
}

val IMAGE_CREATE_FAILED_INVALID_IMAGE = component {
    text("上传的图片无法被处理，请尝试更换文件后重新创建") with mochaMaroon
}

val IMAGE_CREATE_FAILED_SERVER = component {
    text("创建地图画时发生服务器错误，请向管理组反馈") with mochaMaroon
}

val IMAGE_CREATE_SUCCEEDED = component {
    text("√ 地图画 ") with mochaGreen
    text("$IMAGE_PLACEHOLDER_NAME ") with mochaYellow
    text("创建成功，已将物品发给你") with mochaGreen
}

val IMAGE_CREATE_VERIFICATION_FAILED_PREFIX = component {
    text("上传失败: ") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_FILE_TOO_LARGE = component {
    text("文件大小超过限制") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_IMAGE_TOO_LARGE = component {
    text("图片分辨率过大 (") with mochaMaroon
    text("$IMAGE_PLACEHOLDER_IMAGE_WIDTH × $IMAGE_PLACEHOLDER_IMAGE_HEIGHT") with mochaText
    text(")") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_TOO_MANY_FRAMES = component {
    text("动画帧数过多 (") with mochaMaroon
    text(IMAGE_PLACEHOLDER_FRAME_COUNT) with mochaText
    text(")") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_UNALLOWED_EXTENSION = component {
    text("文件扩展名不受支持 (") with mochaMaroon
    text(IMAGE_PLACEHOLDER_FILE_NAME) with mochaText
    text(")") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_UNSUPPORTED_FORMAT = component {
    text("不支持的图片格式") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_CORRUPTED = component {
    text("图片文件已损坏") with mochaMaroon
}

val IMAGE_CREATE_VERIFICATION_FAILED_UNKNOWN = component {
    text("校验上传文件时发生错误") with mochaMaroon
}

val IMAGE_CANCEL_UPLOAD_NO_SESSION = component {
    text("你当前没有未完成的地图画创建请求") with mochaMaroon
}

val IMAGE_ITEM_NAME = component {
    text("$IMAGE_PLACEHOLDER_NAME ") with mochaText
    text("($IMAGE_PLACEHOLDER_WIDTH × $IMAGE_PLACEHOLDER_HEIGHT)") with mochaLavender
}

val IMAGE_ITEM_COPY_RECIPE_RESULT_NAME = component {
    text("复制地图画") with mochaText
}

val IMAGE_ITEM_LORE = listOf(
    component { text("由 $IMAGE_PLACEHOLDER_CREATOR") with mochaSubtext0 without italic() },
    component { text("创建于 $IMAGE_PLACEHOLDER_TIME") with mochaSubtext0 without italic() },
    component { empty() },
    component { text("这是一幅地图画！") with mochaFlamingo without italic() },
    component {
        text("你可以把它放入一面 ") with mochaText without italic()
        text("$IMAGE_PLACEHOLDER_WIDTH × $IMAGE_PLACEHOLDER_HEIGHT ") with mochaLavender without italic()
        text("的展示框内") with mochaText without italic()
    },
    component { empty() },
    component {
        keybind("key.use") with mochaLavender without italic()
        text(" 放入展示框") with mochaText without italic()
    }
)

val IMAGE_ITEM_PLACEMENT_FAILED_INVALID = component {
    newline()
    text("这似乎是一幅无效的地图画...") with mochaMaroon
    newline()
    text("无法找到此物品对应的地图画数据，可能是因为它已被删除。") with mochaSubtext0
    newline()
}

val IMAGE_ITEM_PLACEMENT_FAILED_NO_SPACE_SUBTITLE = component {
    text("空间不足，需要一面 ") with mochaMaroon
    text("$IMAGE_PLACEHOLDER_WIDTH × $IMAGE_PLACEHOLDER_HEIGHT ") with mochaText
    text("的展示框") with mochaMaroon
}

val IMAGE_DATA_MIGRATION_START = component {
    text("开始迁移旧版 ImageData 存储...") with mochaText
}

val IMAGE_DATA_MIGRATION_FINISHED = component {
    text("ImageData 迁移完成：已处理 ") with mochaText
    text("$IMAGE_PLACEHOLDER_PROCESSED ") with mochaLavender
    text("条，成功 ") with mochaText
    text("$IMAGE_PLACEHOLDER_SUCCESS ") with mochaGreen
    text("条，失败 ") with mochaText
    text(IMAGE_PLACEHOLDER_FAILED) with mochaMaroon
    text("条") with mochaText
}

fun getImageDataMigrationFailedMessage(imageId: String, reason: String) = component {
    text("迁移失败：imageId=") with mochaMaroon
    text(imageId) with mochaText
    text("，原因=") with mochaMaroon
    text(reason) with mochaText
}
