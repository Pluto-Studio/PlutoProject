package plutoproject.feature.menu.paper

import org.bukkit.entity.Player
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.startScreen as startManagedScreen
import plutoproject.kernel.api.koinGet

internal fun Player.startScreen(screen: InteractiveScreen) =
    startManagedScreen(koinGet<GuiManager>(), screen)
