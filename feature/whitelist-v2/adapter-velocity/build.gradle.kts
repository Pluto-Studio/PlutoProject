plugins {
    id("plutoproject.adapter-velocity-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:adapter-common"))
    api(project(":framework-velocity-api"))
    api(project(":feature-velocity-api"))
}
