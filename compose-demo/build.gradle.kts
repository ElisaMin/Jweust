@file:Suppress("unstableApiUsage","DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
    alias(libs.plugins.shadowjar)
    id("me.heizi.jweust")
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

tasks {

    getByName("jweust").dependsOn("shadowJar")

    register("run-exe") {
        group = "jweust"
        dependsOn("build","jweust")
        doLast {
            val ext = projectDir.resolve("build/jweust/").listFiles()!!.find { it.name.endsWith(".exe") }!!
            println(ext)
            // println size of exe in mb
            println("exe.size ="+ ext.length().toDouble() / 1024 + "kb")
            val jar = projectDir.resolve("build/libs/").listFiles()!!.find { it.name.endsWith(".jar") }!!
            println("jar.size ="+ jar.length().toDouble() / 1024 + "kb")
            println("size.dst ="+ (ext.length()-jar.length()).toDouble() / 1024 + "kb")
            exec {
                commandLine(ext.absolutePath)
            }
        }
    }

}
tasks.shadowJar {
    this.manifest {
        this.attributes["Main-Class"] = "MainKt"
    }
}
jweust {
    defaults()
    charset {
//        this.pageCode = "65001"
        this.stdout = "GBK"
    }
}