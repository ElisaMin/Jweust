@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    `kotlin-dsl`
    `maven-publish`
//    alias(libs.plugins.compose) apply false
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
        create<JvmTestSuite>("testInGradleRunner") {
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
gradlePlugin.testSourceSets(sourceSets["testInGradleRunner"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("testInGradleRunner"))
}
publishing.publications.all {
    if (this !is MavenPublication) return@all
    pom {
        name = "Jweust"
        description = "A modern Gradle plugin aiming to replace exe4J " +
                "by generating native Windows EXE for JAR files using the Rust language, " +
                "with features like embedding the JAR, logging, and Git repository management."
        url = "https://github.com/ElisaMin/Jweust"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "ElisaMin"
                name = "ElisaMin"
                email = "heizi@lge.fun"
            }
        }
        scm {
            connection = "scm:git:git://github.com/ElisaMin/Jweust.git"
            developerConnection = "scm:git:ssh://github.com:ElisaMin/Jweust.git"
            url = "https://github.com/ElisaMin/Jweust"
        }
    }
}