package plutoproject.kernel.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Feature(
    val id: String,
    val bootstrap: KClass<*> = Nothing::class,
    val platform: Platform,
    val requiredFeatures: Array<String> = [],
    val optionalFeatures: Array<String> = [],
    val requiredCapabilities: Array<String> = [],
)
