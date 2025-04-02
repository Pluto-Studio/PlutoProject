package plutoproject.framework.common.api.bridge

import plutoproject.framework.common.api.bridge.player.PlayerOperationType
import plutoproject.framework.common.api.bridge.server.WorldOperationType
import java.util.*

// 通用
class ServerOfflineException(id: String) : RuntimeException()

class WorldNotFoundException(id: String) : RuntimeException()

class UnsupportedException() : RuntimeException()

// 玩家操作

class PlayerOfflineException(uniqueId: UUID) : RuntimeException()

class PlayerOperationTimeoutException(uniqueId: UUID, type: PlayerOperationType) : RuntimeException()

// 世界操作

class WorldOperationTimeoutException(id: String, type: WorldOperationType) : RuntimeException()
