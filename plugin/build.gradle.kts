@file:Suppress("UnstableApiUsage")

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
}

group = "me.heizi.gradle.plugins"
version = rootProject.libs.versions.jweust.get()

repositories {
    mavenCentral {
        url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
    }
    maven {
        url = uri("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
    }
}
dependencies {
    runtimeOnly("org.slf4j:slf4j-log4j12:+")
    implementation("me.heizi.kotlinx:khell:0.0.1-alpha02")
    implementation("me.heizi.kotlinx:khell-api:0.0.1-alpha02")
}
kotlin {
    jvmToolchain(19)
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
//
gradlePlugin {
    plugins {
        create("jweust") {
            id = "me.heizi.jweust"
            implementationClass = "me.heizi.jweust.JweustPlugin"
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])
//
//tasks.named<Task>("check") {
//    // Include functionalTest as part of the check lifecycle
//    dependsOn(testing.suites.named("functionalTest"))
//}
