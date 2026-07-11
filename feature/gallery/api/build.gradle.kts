plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutine.core)
    api(project(":feature:gallery:core"))
}
