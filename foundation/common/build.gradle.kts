plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(libs.kotlinx.serialization)
    api(libs.mongodb.kotlin.bson)
}
