package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob

/**
 * 插件内部使用的 [CoroutineScope]，会在插件关闭时一并关闭。
 *
 * 请勿使用 [GlobalScope] 来开启协程，因为使用它运行的任务无法随着插件卸载一并被取消。
 */
object PluginScope : CoroutineScope by CoroutineScope(SupervisorJob())
