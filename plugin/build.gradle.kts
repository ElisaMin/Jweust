@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    `kotlin-dsl`
    `maven-publish`
}

group = "me.heizi.gradle.plugins"
version = rootProject.libs.versions.jweust.get()

gradlePlugin {
    plugins {
        create("jweust") {
            id = "me.heizi.jweust"
            implementationClass = "me.heizi.jweust.JweustPlugin"
        }
    }
}
publishing.repositories {
    maven {
        name = "test"
        url = uri("file://${rootProject.projectDir}/build/maven-repo/")
    }
    if (!System.getenv("GITHUB_ACTOR").isNullOrEmpty() && !System.getenv("GITHUB_TOKEN").isNullOrEmpty()) maven {
        name = "github"
        url = uri("https://maven.pkg.github.com/ElisaMin/Khell")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

repositories {
    mavenCentral {
        url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
    }
    maven {
        url = uri("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
    }
}
dependencies {
    implementation(libs.khell.self)
    implementation(libs.khell.api)
}

kotlin {
    jvmToolchain(17)
    target {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xcontext-receivers"
            }
        }
    }
}
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest()
        }
        create<JvmTestSuite>("functionalTest") {
            useKotlinTest()
            dependencies {
                implementation(project())
            }
            targets {
                all {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}
gradlePlugin.testSourceSets(sourceSets["functionalTest"])
//
//tasks.named<Task>("check") {
//    // Include functionalTest as part of the check lifecycle
//    dependsOn(testing.suites.named("functionalTest"))
//}
