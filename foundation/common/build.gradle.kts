plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.mongodb.kotlin.bson)
}
