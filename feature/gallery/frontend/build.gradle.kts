import com.github.gradle.node.npm.task.NpmTask

plugins {
    java
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    download.set(true)
    version.set("22.14.0")
    npmVersion.set("10.9.2")
}

val npmBuild by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "build"))

    inputs.files(
        file("package.json"),
        file("package-lock.json"),
        file("vite.config.js"),
        file("index.html"),
        file("eslint.config.js")
    )
    inputs.dir(file("src"))
    outputs.dir(layout.buildDirectory.dir("dist"))
}

tasks.processResources {
    dependsOn(npmBuild)
    from(layout.buildDirectory.dir("dist")) {
        into("gallery_frontend")
    }
}

tasks.assemble {
    dependsOn(npmBuild)
}
