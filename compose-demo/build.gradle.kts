@file:Suppress("unstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
    alias(libs.plugins.jweust)
    alias(libs.plugins.shadowjar)
}
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.khell.self)
}
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
    maven {
        url = uri("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
    }
}

group = "me.heizi.jweust.demo.compose"
version = "1.0.0"
kotlin {
    jvmToolchain(19)
}
kotlin {
    target { compilations.all {
        kotlinOptions {
            freeCompilerArgs += "-Xcontext-receivers"
        }
    } }
}

compose.desktop {
    application {
        // not working anyways
        mainClass = "MainKt"
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "compose-demo"
//            packageVersion = "1.0.0"
//        }
    }
}
tasks.build {
    // invoke root project's task jar before build
    dependsOn(":jweust:jar", ":jweust:publishToMavenLocal")
}

jweust {
    defaults()
    tasks.shadowJar {
        this.manifest {
            this.attributes["Main-Class"] = "MainKt"
        }
        jar.files = setOf(outputs.files.files.first().canonicalPath)
    }
    charset {
        this.pageCode = "65001"
        this.stdout = "GBK"
    }
}