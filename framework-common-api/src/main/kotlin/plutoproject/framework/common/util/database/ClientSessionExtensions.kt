package plutoproject.framework.common.util.database

import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import java.util.logging.Level
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("PlutoProject/MongoDB")

/**
 * 在当前 [ClientSession] 上开启事务。
 *
 * @param transactionOptions 事务设置
 * @param block 事务逻辑函数
 * @return 事务结果，若失败可获取异常
 */
suspend fun ClientSession.withTransaction(
    transactionOptions: TransactionOptions = TransactionOptions.builder().build(),
    block: suspend (ClientSession) -> Unit
): Result<Unit> = runCatching {
    startTransaction(transactionOptions)
    block(this)
    commitTransaction()
}.onFailure {
    if (hasActiveTransaction()) {
        runCatching { abortTransaction() }.onFailure {
            logger.log(Level.SEVERE, "Error occurred when aborting transaction", it)
        }
    }
}
