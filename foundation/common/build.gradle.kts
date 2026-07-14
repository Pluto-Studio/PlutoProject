plugins {
    id("plutoproject.core")
}

dependencies {
    api(libs.kotlinx.coroutine.core)
    api(libs.kotlinx.serialization.json)
    api(libs.mongodb.kotlin.bson)
    api(libs.adventure)
    api(libs.adventureKt)
    implementation(libs.catppuccin)
    implementation(libs.guava)
    implementation(libs.mongodb.kotlin)
}
