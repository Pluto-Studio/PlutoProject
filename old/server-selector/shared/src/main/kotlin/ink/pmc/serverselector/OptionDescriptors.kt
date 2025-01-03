package ink.pmc.serverselector

import ink.pmc.framework.options.EntryValueType
import ink.pmc.framework.options.dsl.descriptor

val AUTO_JOIN_DESCRIPTOR = descriptor<Boolean> {
    key = "server_selector.auto_join"
    type = EntryValueType.BOOLEAN
    defaultValue = false
}