plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(libs.kotlin.stdlib)
    api(project(":feature:whitelist-v2:core"))
}
