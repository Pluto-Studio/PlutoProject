package plutoproject.framework.common.api.connection

import club.plutoproject.charonflow.CharonFlow
import plutoproject.framework.common.util.inject.Koin

/**
 * CharonFlow 通信框架外部连接。
 */
interface CharonFlowConnection {
    companion object : CharonFlowConnection by Koin.get()

    /**
     * 连接的 [CharonFlow]。
     */
    val client: CharonFlow
}
