package plutoproject.framework.common.api.feature.annotation

import plutoproject.framework.common.api.feature.Platform

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Feature(
    val id: String,
    val platform: Platform,
    val dependencies: Array<Dependency> = [],
)
