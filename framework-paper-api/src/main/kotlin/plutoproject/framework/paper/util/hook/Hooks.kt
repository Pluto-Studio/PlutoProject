package plutoproject.framework.paper.util.hook

import plutoproject.framework.paper.util.server

var vaultHook: VaultHook? = null

fun initHooks() {
    if (server.pluginManager.getPlugin("Vault") != null) {
        vaultHook = VaultHook()
    }
}
