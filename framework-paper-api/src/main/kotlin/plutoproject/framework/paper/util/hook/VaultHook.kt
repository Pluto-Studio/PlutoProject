package plutoproject.framework.paper.util.hook

import net.milkbowl.vault.economy.Economy
import plutoproject.framework.paper.util.server

class VaultHook {
    val economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
}
