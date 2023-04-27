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
    `use heizi's maven repo by github`()
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
        mainClass = "MainKt"
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "compose-demo"
//            packageVersion = "1.0.0"
//        }
    }
}

jweust {
    defaults()
    tasks.shadowJar {
        jar.files = setOf(outputs.files.files.first().canonicalPath)
    }
}