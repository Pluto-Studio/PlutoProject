plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(project(":capability:mongo:common"))
    implementation(project(":kernel:api"))
}
