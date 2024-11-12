package ink.pmc.framework.bridge.server

enum class ServerState {
    LOCAL, REMOTE;

    val isLocal: Boolean
        get() = this == LOCAL
    val isRemote: Boolean
        get() = this == REMOTE
}