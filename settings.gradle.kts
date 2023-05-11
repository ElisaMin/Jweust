rootProject.name = "Jweust"
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
        }
    }
}
include(
    "plugin"
)
project(":plugin").name = "jweust"