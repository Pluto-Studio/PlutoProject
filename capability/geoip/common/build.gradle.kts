plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.kotlin-test")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.geoip.api)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.koin.core)
    implementation(libs.geoip2)
}
