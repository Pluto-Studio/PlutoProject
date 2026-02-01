package plutoproject.feature.common.whitelist_v2

import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.whitelist_v2.model.VisitorRecordModel
import plutoproject.feature.common.whitelist_v2.repository.VisitorRecordRepository
import plutoproject.feature.common.whitelist_v2.repository.WhitelistRecordRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.util.coroutine.PluginScope

private const val WHITELIST_PREFIX = "whitelist_v2_"
private const val WHITELIST_RECORD_COLLECTION = "whitelist_records"
private const val VISITOR_RECORD_COLLECTION = "visitor_records"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return MongoConnection.getCollection<T>("$WHITELIST_PREFIX$name")
}

private fun createVisitorRecordIndexes(collection: MongoCollection<*>) = PluginScope.launch(Dispatchers.IO) {
    collection.createIndex(
        Indexes.compoundIndex(
            Indexes.ascending("ipAddress.ipVersion"),
            Indexes.ascending("ipAddress.ipHigh"),
            Indexes.ascending("ipAddress.ipLow")
        )
    )
}

val whitelistCommonModule = module {
    single<Whitelist> { WhitelistImpl() }
    single<WhitelistRecordRepository> {
        WhitelistRecordRepository(getCollection(WHITELIST_RECORD_COLLECTION))
    }
    single<VisitorRecordRepository> {
        val collection = getCollection<VisitorRecordModel>(VISITOR_RECORD_COLLECTION)
        createVisitorRecordIndexes(collection)
        VisitorRecordRepository(collection)
    }
}
