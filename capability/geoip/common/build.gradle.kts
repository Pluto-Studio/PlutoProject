plugins {
    id("plutoproject.core")
    id("plutoproject.test")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.geoip.api)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.koin.core)
    implementation(libs.geoip2)
}
