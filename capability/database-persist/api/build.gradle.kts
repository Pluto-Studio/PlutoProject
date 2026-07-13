plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.mongodb.kotlin.bson)
}
