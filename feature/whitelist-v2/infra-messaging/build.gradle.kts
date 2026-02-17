plugins {
    id("plutoproject.core-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:core"))

    api(project(":framework-common-api"))
}
