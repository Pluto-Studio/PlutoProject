package ink.pmc.essentials.velocity

data class EssentialsProxyConfig(
    val message: Message,
)

data class Message(
    val enabled: Boolean = true,
    val join: String = "<c:#a6e3a1>[+]</c> <c:#f9e2af>\$player</c>",
    val quit: String = "<c:#eba0ac>[-]</c> <c:#f9e2af>\$player</c>"
)