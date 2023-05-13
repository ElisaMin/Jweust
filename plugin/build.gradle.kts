@file:Suppress("UnstableApiUsage")

group = "me.heizi.gradle.plugins"
version = rootProject.libs.versions.jweust.get()

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.publish)
}
repositories {
    mavenCentral()
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

object Args {
    const val website = "https://github.com/ElisaMin/Jweust"
    const val name = "Jweust"
    const val description = "A modern Gradle plugin aiming to replace exe4J " +
            "by generating native Windows EXE for JAR files using the Rust language, " +
            "with features like embedding the JAR, logging, and Git repository management."
}
gradlePlugin {
    website = Args.website
    vcsUrl = Args.website+".git"
    plugins {
        create("jweust") {
            id = "me.heizi.jweust"
            implementationClass = "me.heizi.jweust.JweustPlugin"
            displayName = Args.name
            website = Args.website
            description = Args.description
            tags = listOf("jar", "exe", "rust", "windows", "launcher","jar-launcher")
        }
    }
}
publishing.repositories {
    maven {
        name = "test"
        url = uri("file://${rootProject.projectDir}/build/maven-repo/")
    }
}
publishing.publications.all {
    if (this !is MavenPublication) return@all
    pom {
        name = Args.name
        description = Args.description
        url = Args.website
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
            url = Args.website
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
