package plutoproject.feature.motd.velocity

import net.kyori.adventure.text.Component
import plutoproject.foundation.common.text.mochaGreen
import plutoproject.foundation.common.text.mochaMaroon

val MOTD_RELOAD_SUCCESS: Component = Component.text("MOTD 配置已重载", mochaGreen)
val MOTD_RELOAD_FAILURE: Component = Component.text("MOTD 配置重载失败，请检查控制台报错", mochaMaroon)
