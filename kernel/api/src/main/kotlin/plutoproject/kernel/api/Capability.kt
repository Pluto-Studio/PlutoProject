package plutoproject.kernel.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Capability(
    val id: String,
    val bootstrap: KClass<*> = Nothing::class,
    val platform: Platform,
    val requiredCapabilities: Array<String> = [],
)
