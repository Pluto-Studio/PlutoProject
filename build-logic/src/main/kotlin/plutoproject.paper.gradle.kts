import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("plutoproject.kotlin-library")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    with(extensions.getByType<PaperweightUserDependenciesExtension>()) {
        paperDevBundle("26.2.build.38-alpha")
    }
}
