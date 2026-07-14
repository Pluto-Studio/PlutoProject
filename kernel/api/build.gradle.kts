plugins {
    id("plutoproject.core")
    kotlin("plugin.serialization")
}

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutine.core)
    api(libs.kotlinx.serialization.json)
    api(libs.koin.core)
}
