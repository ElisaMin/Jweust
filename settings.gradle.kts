rootProject.name = "Jweust"
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            name = "test"
            url = uri("file://${rootProject.projectDir}/build/maven-repo/")
        }
    }
}
include(
    "plugin",
    "compose-demo"
)