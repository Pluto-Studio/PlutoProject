package ink.pmc.hypervisor

import ink.pmc.framework.options.EntryValueType
import ink.pmc.framework.options.dsl.descriptor

val DYNAMIC_VIEW_DISTANCE = descriptor<Boolean> {
    key = "hypervisor.dynamic_view_distance"
    type = EntryValueType.BOOLEAN
    defaultValue = false
}