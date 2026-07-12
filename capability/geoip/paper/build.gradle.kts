plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:geoip:common"))
    implementation(project(":kernel:api"))
}
