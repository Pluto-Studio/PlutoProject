plugins {
    id("plutoproject.paper")
}

dependencies {
    api(projects.feature.teleport.api.paper)
    api(projects.capability.profile.api)
}
