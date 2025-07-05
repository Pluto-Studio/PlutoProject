package plutoproject.feature.paper.api.sit.player

import plutoproject.framework.common.util.inject.Koin

interface PlayerSit {
    companion object : PlayerSit by Koin.get()
}
