rootProject.name = "compose-demo"
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
//version catalog set lib to ../gradle/libs.versions.toml
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
includeBuild("..")
