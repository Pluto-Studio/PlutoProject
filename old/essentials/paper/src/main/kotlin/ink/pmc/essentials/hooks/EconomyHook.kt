package ink.pmc.essentials.hooks

import ink.pmc.framework.platform.paper
import net.milkbowl.vault.economy.Economy

class EconomyHook {
    val economy = paper.servicesManager.getRegistration(Economy::class.java)?.provider
}