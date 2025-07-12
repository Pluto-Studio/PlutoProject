package plutoproject.feature.paper.menu.repositories

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.toList
import plutoproject.feature.paper.menu.models.UserModel

class UserRepository(private val collection: MongoCollection<UserModel>) {
    suspend fun find(): List<UserModel> {
        return collection.find().toList()
    }
}
