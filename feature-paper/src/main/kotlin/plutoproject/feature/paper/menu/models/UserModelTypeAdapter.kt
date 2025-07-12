package plutoproject.feature.paper.menu.models

import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.api.databasepersist.adapters.SerializationTypeAdapter

object UserModelTypeAdapter : DataTypeAdapter<PersistUserModel> by SerializationTypeAdapter()
