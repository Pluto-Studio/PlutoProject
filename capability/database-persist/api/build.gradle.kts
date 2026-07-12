plugins {
    id("plutoproject.kotlin-library")
}

dependencies {
    api(project(":foundation:common"))
    api(libs.mongodb.kotlin.bson)
}
