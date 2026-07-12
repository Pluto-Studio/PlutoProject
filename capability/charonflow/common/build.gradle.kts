plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.kotlin-test")
}

dependencies {
    api(project(":capability:charonflow:api"))
    api(project(":kernel:api"))
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.koin.core)
}
