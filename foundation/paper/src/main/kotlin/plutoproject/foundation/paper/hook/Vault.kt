package plutoproject.foundation.paper.hook

import net.milkbowl.vault.economy.Economy
import org.bukkit.Server

val Server.vaultEconomy: Economy?
    get() = servicesManager.getRegistration(Economy::class.java)?.provider
