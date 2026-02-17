package plutoproject.feature.whitelist_v2.adapter.common

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.feature.whitelist_v2.core.usecase.*
import plutoproject.feature.whitelist_v2.infra.mongo.MongoVisitorRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.MongoWhitelistRecordRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.util.coroutine.PluginScope
import java.time.Clock

private const val WHITELIST_PREFIX = "whitelist_v2_"
private const val WHITELIST_RECORD_COLLECTION = "whitelist_records"
private const val VISITOR_RECORD_COLLECTION = "visitor_records"

private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
    return MongoConnection.getCollection("$WHITELIST_PREFIX$name")
}

val commonModule = module {
    single { Clock.systemUTC() }
    single { KnownVisitors() }

    single<WhitelistRecordRepository> {
        MongoWhitelistRecordRepository(getCollection(WHITELIST_RECORD_COLLECTION))
    }
    single<VisitorRecordRepository> {
        val repo = MongoVisitorRecordRepository(getCollection(VISITOR_RECORD_COLLECTION))
        PluginScope.launch(Dispatchers.IO) {
            repo.ensureIndexes()
        }
        repo
    }

    singleOf(::IsWhitelistedUseCase)
    singleOf(::LookupWhitelistRecordUseCase)
    singleOf(::GrantWhitelistUseCase)
    singleOf(::RevokeWhitelistUseCase)
    singleOf(::LookupVisitorRecordUseCase)
    singleOf(::CreateVisitorRecordUseCase)
    singleOf(::LookupVisitorRecordsByCidrUseCase)
    singleOf(::LookupVisitorRecordsByIpUseCase)

    singleOf(::WhitelistService) bind Whitelist::class
}
