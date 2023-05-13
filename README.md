# Jweust !!! Gradle Plugin
**J**ava**W**indows**Exe**cutableR**ust** is a gradle plugin allows you create an EXE for Java ~~GUI only maybe~~ applications.  
## Notes or Features
### Plugin Notes
 * In this plugin, the EXEs is compiled by `Rust-lang`. 
 * once started, it'll clone a template rust project by git, into the directory in buildDir/tmp/jweust of Root Project by default. (but you can define it on gradle.build ~~.kts~~ as you want).
 * base on repository (as a rust project) in `JweustExtention.JweustRoot` dir, in runtime, it'll create a git branch of the subprojects, and commit the changes to the branch, check,**fetch and merge the newer tag is released***.
 * to configuration the rust, it'll generate the rust files from (tasks or gradle)'s configuration automatically.
 
### RustTemplate Features
- Use Rust to create an exe launcher for Java programs.
- Get configuration from Gradle build tasks and convert to Rust code (var.rs).
- The var.rs file contains options such as working directory, log path, and Java options.
- Directly call Java.exe to run the jar.
- Error message box.
- Singleton EXE supported.
- Embed JRE supported.

# Getting Started

## Installing
it's not released yet, so you can clone this repository and run `gradlew publishToMavenLocal` to install it to your local maven repository.

## Usage
### 1. Add the plugin to your project
```toml
[versions]
jweust = "0.0.1"
[plugins]
jweust = { id = "me.heizi.jweust", version.ref = "jweust" }
```
### 2. configuration
> warning: the configuration is not stable yet, it may change in the future.  
```kotlin
plugins {
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.jweust) // or id("me.heizi.jweust")
}
// ...configuration jar before jweust
shadowJar {
    mainClass = "..."
}
jweust {
    defaults()
    JweustRoot = file("...") // default is buildDir/tmp/jweust
    rustProjectName = "..." // default is project.name.snackCased
    version = "..." // default is project.version.`parse to x.x.x`
    jre {
        search += JvmDir(".runtime/") // search jre in exe locate/.runtime/ dir
        search += EnvVar("JDK_17_HOME") // it will search JAVA_HOME by default
        min = "17" // min version
        max = "17" // max version
    }
    jar {
        // jar file to run
        // it will use shadowJar's output or jar's by default
        // if you want to use other jars, you can set it here
        files += project.tasks.named<ShadowJar>("shadowJar").get().archiveFile.get().asFile
        laucnher {
            file = 0 // index of files
            args = mapOf(-1 to "arg",Int::MAX_VALUE to "--Xmx-OrSomething" ) // args to pass to java
            mainClass // ....
        }
    }
    exe {
        // define the information of exe
        // see Microsoft's document for more information `windows executable resource `
        icon = File("...")
    }
    charset {
        jvm = "...".toCharset() 
    }
    // ...other configuration
    // see JweustExtension for more information
    
}
```
### 3. run
```shell
gradlew jweust
# or
gradlew configExe complieExe
```