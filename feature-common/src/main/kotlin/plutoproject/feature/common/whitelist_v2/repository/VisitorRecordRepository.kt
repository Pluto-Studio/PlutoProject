package plutoproject.feature.common.whitelist_v2.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import plutoproject.feature.common.whitelist_v2.model.VisitorRecordModel

class VisitorRecordRepository(private val collection: MongoCollection<VisitorRecordModel>) {

}
