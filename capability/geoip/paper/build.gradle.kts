plugins {
    id("plutoproject.core")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.geoip.common)
}
