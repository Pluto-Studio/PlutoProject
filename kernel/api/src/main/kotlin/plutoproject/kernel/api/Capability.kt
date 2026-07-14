package plutoproject.kernel.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Capability(
    val id: String,
    val platform: Platform,
    val requiredCapabilities: Array<String> = [],
)
