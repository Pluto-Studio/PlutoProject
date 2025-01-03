package plutoproject.framework.common.util.inject

import org.koin.core.component.KoinComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T> inlinedGet(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T {
    return Koin.get(qualifier, parameters)
}

@Suppress("UNUSED")
inline fun <reified T : Any> KoinComponent.getOrNull(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T? {
    return getKoin().getOrNull<T>(qualifier, parameters)
}
