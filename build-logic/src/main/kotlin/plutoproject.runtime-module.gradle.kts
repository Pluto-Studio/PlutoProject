plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":kernel:module-processor"))
}

ksp {
    arg("runtimeModule.projectPath", project.path)
}
