package plutoproject.feature.menu.paper.models

import plutoproject.capability.databasepersist.api.DataTypeAdapter
import plutoproject.capability.databasepersist.api.adapters.SerializationTypeAdapter

object UserModelTypeAdapter : DataTypeAdapter<PersistUserModel> by SerializationTypeAdapter()
