plugins {
    id("plutoproject.core")
}

dependencies {
    implementation(projects.foundation.common)
    implementation(libs.mongodb.kotlin.bson)
}
