plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
}
dependencies {
    implementation(compose.desktop.currentOs)
}
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

group = "me.heizi.jweust.demo.compose"
version = "1.0.0"

kotlin {
    jvmToolchain(19)
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
