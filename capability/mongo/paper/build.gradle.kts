plugins {
    id("plutoproject.kotlin-library")
    id("plutoproject.runtime-module")
}

dependencies {
    implementation(projects.kernel.api)
    implementation(projects.capability.mongo.common)
}
