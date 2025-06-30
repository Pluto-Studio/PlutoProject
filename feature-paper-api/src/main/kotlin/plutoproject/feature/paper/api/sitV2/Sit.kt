package plutoproject.feature.paper.api.sitV2

import plutoproject.framework.common.util.inject.Koin

interface Sit {
    companion object : Sit by Koin.get()
}
