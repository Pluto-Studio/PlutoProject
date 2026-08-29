import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("plutoproject.core")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    with(extensions.getByType<PaperweightUserDependenciesExtension>()) {
        paperDevBundle(group = "club.plutoproject.nix", version = libs.versions.nix.get())
    }
}
