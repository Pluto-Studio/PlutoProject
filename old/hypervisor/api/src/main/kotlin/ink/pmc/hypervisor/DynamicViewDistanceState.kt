package ink.pmc.hypervisor

/*
* ENABLED -> Options 中开启
* DISABLED -> Options 中关闭
* DISABLED_DUE_PING -> 延迟不符合，本地关闭
* ENABLED_BUT_DISABLED_DUE_PING -> 此前是开启状态，因延迟不符合，本地关闭
* DISABLED_DUE_VHOST -> 虚拟主机不符合，本地关闭
* 需要注意的是，本地关闭的优先级更高；
* 即便用户没有在 Options 开启动态视距，也可能会是 DISABLED_DUE_PING 或 DISABLED_DUE_VHOST。
* */
enum class DynamicViewDistanceState {
    ENABLED, DISABLED, DISABLED_DUE_PING, ENABLED_BUT_DISABLED_DUE_PING, DISABLED_DUE_VHOST;

    val isDisabledLocally: Boolean
        get() = when (this) {
            ENABLED -> false
            DISABLED -> false
            DISABLED_DUE_PING -> true
            DISABLED_DUE_VHOST -> true
            ENABLED_BUT_DISABLED_DUE_PING -> true
        }
}