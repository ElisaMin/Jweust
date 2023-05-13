rootProject.name = "Jweust"
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
include(
    "plugin"
)
project(":plugin").name = "jweust"