plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":build-support:module-processor"))
}

ksp {
    arg("runtimeModule.projectPath", project.path)
}
