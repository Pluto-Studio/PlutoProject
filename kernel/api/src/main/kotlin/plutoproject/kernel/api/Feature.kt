package plutoproject.kernel.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Feature(
    val id: String,
    val platform: Platform,
    val requiredFeatures: Array<String> = [],
    val optionalFeatures: Array<String> = [],
    val requiredCapabilities: Array<String> = [],
)
