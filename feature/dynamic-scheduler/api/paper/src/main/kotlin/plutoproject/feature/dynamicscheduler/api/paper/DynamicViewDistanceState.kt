package plutoproject.feature.dynamicscheduler.api.paper

/**
 * 用于表示用户动态视距功能的状态。
 * 需要注意的是，本地关闭的优先级更高；
 * 即便用户没有在 Options 开启动态视距，也可能会是 DISABLED_DUE_PING 或 DISABLED_DUE_VHOST。
 */
enum class DynamicViewDistanceState {
    /**
     * 在 Options 中开启。
     */
    ENABLED,

    /**
     * 在 Options 中关闭。
     */
    DISABLED,

    /**
     * 不符合延迟要求，本地关闭。
     */
    DISABLED_DUE_PING,

    /**
     * 此前是开启状态，因不符合延迟要求，本地关闭。
     */
    ENABLED_BUT_DISABLED_DUE_PING,

    /**
     * 不符合虚拟主机要求，本地关闭。
     */
    DISABLED_DUE_VHOST;

    /**
     * 判断状态是否为本地关闭。
     */
    val isDisabledLocally: Boolean
        get() = when (this) {
            ENABLED -> false
            DISABLED -> false
            DISABLED_DUE_PING -> true
            DISABLED_DUE_VHOST -> true
            ENABLED_BUT_DISABLED_DUE_PING -> true
        }
}
