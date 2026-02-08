plugins {
    id("plutoproject.test-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))

    testImplementation(libs.kotlinx.coroutine.core)
}
