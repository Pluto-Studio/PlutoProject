plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.mongodb.kotlin.bson)
    api(libs.adventure)
    api(libs.adventureKt)
    implementation(libs.catppuccin)
}
