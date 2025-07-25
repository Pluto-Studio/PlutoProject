package plutoproject.framework.common.util.database

import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession

suspend inline fun ClientSession.withTransaction(
    transactionOptions: TransactionOptions = TransactionOptions.builder().build(),
    block: () -> Unit
) {
    startTransaction(transactionOptions)
    block()
    commitTransaction()
}
