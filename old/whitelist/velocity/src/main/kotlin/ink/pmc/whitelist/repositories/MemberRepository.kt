package ink.pmc.whitelist.repositories

import com.mongodb.kotlin.client.coroutine.MongoCollection
import ink.pmc.whitelist.models.MemberModel
import kotlinx.coroutines.flow.toList

/*
* 用于迁移老 Member 系统
* */
class MemberRepository(private val collection: MongoCollection<MemberModel>) {
    suspend fun list(): List<MemberModel> {
        return collection.find().toList()
    }
}