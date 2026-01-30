package plutoproject.feature.common.whitelist_v2.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import plutoproject.feature.common.whitelist_v2.model.WhitelistRecordModel

class WhitelistRecordRepository(private val collection: MongoCollection<WhitelistRecordModel>) {

}
