package plutoproject.framework.common.util.jvm

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent

val ByteBuddy = ByteBuddy().also {
    ByteBuddyAgent.install()
}
