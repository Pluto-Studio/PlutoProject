package plutoproject.feature.paper.menu

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.menu.models.PersistUserModel
import plutoproject.feature.paper.menu.models.UserModel
import plutoproject.feature.paper.menu.models.UserModelTypeAdapter
import plutoproject.feature.paper.menu.repositories.UserRepository
import plutoproject.framework.common.api.databasepersist.DatabasePersist

class PersistMigrator : KoinComponent {
    private val repository by inject<UserRepository>()

    suspend fun migrate(): List<UserModel> {
        val models = repository.find()
        models.forEach { model ->
            val container = DatabasePersist.getContainer(model.uuid)
            val persistModel = PersistUserModel(model.wasOpenedBefore, model.itemGivenServers)
            container.set(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, persistModel)
            container.save()
        }
        return models
    }
}
