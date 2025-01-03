package plutoproject.framework.common.util.data.flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

operator fun <T, V> StateFlow<V>.getValue(t: T, property: KProperty<*>): V {
    return value
}

operator fun <T, V> MutableStateFlow<V>.setValue(t: T, property: KProperty<*>, value: V) {
    this.value = value
}
