package plutoproject.framework.common.api.bridge.world

import plutoproject.framework.common.api.bridge.server.ServerElement

interface WorldElement<T : WorldElement<T>> : ServerElement<T> {
    val world: BridgeWorld?
}
