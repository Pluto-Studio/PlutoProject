plugins {
    id("plutoproject.core-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:application"))
}
