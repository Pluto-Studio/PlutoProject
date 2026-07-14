package plutoproject.feature.menu.paper

import plutoproject.kernel.api.koinInject
import plutoproject.feature.menu.paper.models.PersistUserModel
import plutoproject.feature.menu.paper.models.UserModel
import plutoproject.feature.menu.paper.models.UserModelTypeAdapter
import plutoproject.feature.menu.paper.repositories.UserRepository
import plutoproject.capability.databasepersist.api.DatabasePersist

class PersistMigrator : Any() {
    private val repository by koinInject<UserRepository>()

    suspend fun migrate(): List<UserModel> {
        val models = repository.find()
        models.forEach { model ->
            val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(model.uuid)
            val persistModel = PersistUserModel(model.wasOpenedBefore, model.itemGivenServers)
            container.set(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, persistModel)
            container.save()
        }
        return models
    }
}
