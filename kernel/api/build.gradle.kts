plugins {
    id("plutoproject.kotlin-library")
    kotlin("plugin.serialization")
}

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutine.core)
    api(libs.kotlinx.serialization)
}
