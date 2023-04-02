package me.heizi.jweust

import org.gradle.api.Project
import java.io.File

val runLikeHandSonFire = NotImplementedError("Maybe tomorrow you impl this ~")

data class JarToExeConfig(
    var includeJar: Boolean = false,
    var applicationType: ApplicationType = ApplicationType.Application(),
    var workdir: File = File("."),
    var log: LogConfig = LogConfig(),
    var exe: ExeConfig = ExeConfig(),
    var jar: JarConfig = JarConfig(),
    var jre: JreConfig = JreConfig(),
    var splashScreen: SplashScreenConfig? = null
) {
    fun log(block: LogConfig.() -> Unit) {
        log = LogConfig().apply(block)
    }
    fun exe(block: ExeConfig.() -> Unit) {
        exe = ExeConfig().apply(block)
    }
    fun jar(block: JarConfig.() -> Unit) {
        jar = JarConfig().apply(block)
    }
    fun jre(block: JreConfig.() -> Unit) {
        jre = JreConfig().apply(block)
    }
    fun splashScreen(block: SplashScreenConfig.() -> Unit) {
        splashScreen = SplashScreenConfig().apply(block)
    }


}

sealed interface ApplicationType {
    @Deprecated("maybe tomorrow you impl this~", ReplaceWith("Application"),DeprecationLevel.ERROR)
    object Console : ApplicationType

    data class Application(
        val enableConsoleArg:String?=null
    ) : ApplicationType {
        init {
            require(
                enableConsoleArg==null
            ) {
                runLikeHandSonFire
            }
        }
    }
    @Deprecated("maybe tomorrow you impl this~", ReplaceWith("Application"),DeprecationLevel.ERROR)
    class Service private constructor() : ApplicationType {
        init {
            throw NotImplementedError("Maybe tomorrow you impl this ~")
        }

    }
}

data class LogConfig(
    var error: LogFileConfig = LogFileConfig(),
    var stdout: File? = null
)

data class LogFileConfig(
    val path: String = "error.log",
    val isOverwrite: Boolean = false
)

enum class Permissions {
    Default,
    HighersInTheRoom,
    Administrator,
}
data class ExeConfig(
    var instance: Boolean = true,
    var x86: Boolean = false,
    var permissions: Permissions = Permissions.Default,
    var icon: File? = null,
    var fileVersion: String = "",
    var productVersion: String = "",
    var internalName: String = "",
    var fileDescription: String = "",
    var legalCopyright: String = "",
    var companyName: String = ""
)

data class JarConfig(
    var files: List<File> = emptyList(),
    var launcher: LauncherConfig? = null
)

data class LauncherConfig(
    var file: File = File(""),
    var mainClass: String? = null,
    var args: Map<Int, String> = emptyMap()
)

data class JreConfig(
    var search: MutableSet<JvmSearch> = mutableSetOf(),
    var options: MutableSet<String> = mutableSetOf(),
    var nativeLibsPath: MutableSet<String> = mutableSetOf(),
    var version: Set<Number> = emptySet(),
    val preferred: JrePreferred = JrePreferred.DefaultVM
) {
    infix fun version(range: ClosedFloatingPointRange<Float>) {
        version = sequenceOf(1.6f,1.7f,1.8f,9f,10f,11f,12f,13f,14f,15f,16f,17f,18f,19f,20f,21f,22f,23f,24f)
            .filter { it in range }.toSet()
    }
}

sealed class JrePreferred {
    object DefaultVM : JrePreferred()
    object ClientHotspot : JrePreferred()
    object ServerHotspot : JrePreferred()
}

data class SplashScreenConfig(
    var imageFile: File = File(""),
    var startLine: SplashScreenText? = null,
    var versionLine: SplashScreenText? = null
)

data class SplashScreenText(
    var text: String = "",
    var fontName: String = "SansSerif",
    var fontSize: Int = 12,
    var fontStyle: Int = 0,
    var color: Int = -1
)
sealed interface JvmSearch {
    data class JvmDir(val path: String):JvmSearch
    data class EnvVar(val name: String):JvmSearch
}



// Define the DSL implementation function

fun Project.jarToExe(configure: JarToExeConfig.() -> Unit) {
    val config = JarToExeConfig().apply(configure)



    // Plugin implementation goes here
}
