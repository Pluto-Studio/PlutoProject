plugins {
    id("plutoproject.adapter-paper-conventions")
}

dependencies {
    api(project(":feature:gallery:adapter-common"))
    api(project(":framework-paper-api"))
    api(project(":feature-paper-api"))
}
