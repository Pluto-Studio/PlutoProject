import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("plutoproject.core")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    with(extensions.getByType<PaperweightUserDependenciesExtension>()) {
        paperDevBundle(libs.versions.paper.get())
    }
}
