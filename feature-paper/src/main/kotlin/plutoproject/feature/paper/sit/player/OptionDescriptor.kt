package plutoproject.feature.paper.sit.player

import plutoproject.framework.common.api.options.EntryValueType
import plutoproject.framework.common.api.options.dsl.OptionDescriptor

val PlayerSitOptionDescriptor = OptionDescriptor<Boolean> {
    key = "sit.player_sit"
    type = EntryValueType.BOOLEAN
    defaultValue = false
}
