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
        maven {
            url = uri("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
        }
    }
}
include(
    "plugin",
    "compose-demo"
)
project(":plugin").name = "jweust"