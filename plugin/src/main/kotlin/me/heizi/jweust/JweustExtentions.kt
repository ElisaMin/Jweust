package me.heizi.jweust


import org.gradle.api.Project


internal fun JweustConfig.getRustFile():String {
    return arrayOf(
        this,
        log,
        exe,
        jar, jar.launcher ?: LauncherConfig(),
        jre,
        charset,
        (splashScreen ?: SplashScreenConfig(null)),
    )
        .asSequence()
        .map { it.parse() }
        .flatMap { it.lines() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}
internal val Project.Jweust get() = JweustConfig().apply {
    rustProjectName = name
    exe {
        group.toString().let { owner ->
            companyName = owner
            legalCopyright = owner
        }
        version.toString().let {version->
            fileVersion = version
            productVersion = version
        }
        name.let {
            productName = it
            internalName = it
        }

        fileDescription = description?:""
    }
    jar {
        fun foundByOutputFiles(name:String) =
        tasks.findByName(name)
            ?.outputs?.files?.firstOrNull()
            ?.let { files = setOf(it.name) }
        foundByOutputFiles("shadowJar") ?:
        foundByOutputFiles("jar")
        Runtime.version().let {
            jre.version += it.feature()
        }
    }

}
data class JweustConfig(
//    var includeJar: Boolean = false,
    override var rustProjectName:String = "",
    // pub const APPLICATION_WITH_OUT_CLI:Option<Option<&'static str>> = Some(Some("-DConsolog=true"));
    @RustParsable.Name("APPLICATION_WITH_OUT_CLI")
    @RustParsable.Type("Option<Option<&'static str>>")
    override var applicationType: ApplicationType = ApplicationType.ConsoleWhileOptionApplication(),
    override var workdir: String = "",
    override var log: LogConfig = LogConfig(),
    override var exe: ExeConfig = ExeConfig(),
    override var jar: JarConfig = JarConfig(),
    override var jre: JreConfig = JreConfig(),
    override var charset: CharsetConfig = CharsetConfig(),
    override var splashScreen: SplashScreenConfig? = null
):RustParsable,JweustVarsExtension {
    override fun parsingValueExtra(name: String): (() -> String)? {
        if (name == "applicationType") return {
            when (val t = applicationType) {
                is ApplicationType.Console -> "None"
                is ApplicationType.Application -> "Some(None)"
                is ApplicationType.ConsoleWhileOptionApplication ->
                    "Some(Some(\"${t.whileCommand}\"))"
            }
        }
        return super.parsingValueExtra(name)
    }
}


sealed interface ApplicationType {
    object Console:ApplicationType
    object Application:ApplicationType
    class ConsoleWhileOptionApplication(
        val whileCommand: String = "-DConsole=true"
    ):ApplicationType

}
//pub const LOG_STDERR_PATH:Option<(Option<&'static str>,bool)> = None;
//pub const LOG_STDOUT_PATH:Option<(Option<&'static str>,bool)> = None;
@RustParsable.Prefix("LOG_")
data class LogConfig(

    @RustParsable.Name("STDERR_PATH")
    @RustParsable.Type("Option<(Option<&'static str>,bool)>")
    var error: LogFileConfig? = null,

    @RustParsable.Name("STDOUT_PATH")
    @RustParsable.Type("Option<(Option<&'static str>,bool)>")
    var stdout: LogFileConfig? = null

):RustParsable {
    override fun parsingValueExtra(name: String): (() -> String) {
        return {
            with(if (name == "error") error else stdout) {
                if (this == null) {
                    "None"
                } else {
                    "Some((\"${this.path}\",${this.isOverwrite}))"
                }
            }
        }
    }
}

data class LogFileConfig(
    val path: String? = GENERATE_FILE,
    val isOverwrite: Boolean = false
) {
    companion object {
        val GENERATE_FILE = null
    }
}

enum class Permissions {
    Default,
    HighersInTheRoom,
    Administrator,
}
//pub const EXE_IS_INSTANCE:bool = false;
//pub const EXE_PERMISSION:i8 = -1;
//pub const EXE_ICON_PATH:Option<&str> = Some("D:\\Downloads\\ic_ast_ugly.ico");
//pub const EXE_FILE_VERSION:&str = "0.0.0.9";
//pub const EXE_PRODUCT_VERSION:&str = "0.0.9";
//pub const EXE_INTERNAL_NAME:&str = "Android apk Sideload Tool From Heizi Flash Tools";
//pub const EXE_FILE_DESCRIPTION:&str = "线刷APK安装工具";
//pub const EXE_LEGAL_COPYRIGHT:&str = "Github/ElisaMin";
//pub const EXE_COMPANY_NAME:& str = "Heizi";
//
//// new
//pub const EXE_ARCH:&str = "x86_64";
//pub const EXE_PRODUCT_NAME:&str = "Android Package Sideload Tool";
@RustParsable.Prefix("EXE_")
data class ExeConfig(
    var isInstance: Boolean = true,
    var arch: String = "x86_64",
    @RustParsable.Type("i8")
    var permissions: Permissions = Permissions.Default,
    var icon: String? = null,
    var fileVersion: String = "",
    var productVersion: String = "",
    var productName: String = "",
    var internalName: String = "",
    var fileDescription: String = "",
    var legalCopyright: String = "",
    var companyName: String = ""
) : RustParsable {
    override fun parsingValueExtra(name: String): (() -> String) {
        return when (name) {
            "permissions" -> {{
                when (permissions) {
                    Permissions.HighersInTheRoom -> "2"
                    Permissions.Administrator -> "1"
                    else -> "3"
                } } }
            else -> error("")
        }
    }
}

//pub const JAR_FILES:&[&str] = &[r#"./lib/AndroidPackageSideloadTool.jar"#];
@RustParsable.Prefix("JAR_")
data class JarConfig(
    var files: Set<String> = emptySet(),
    var launcher: LauncherConfig? = null
):RustParsable

//pub const JAR_LAUNCHER_FILE:usize = 0;
//pub const JAR_LAUNCHER_MAIN_CLASS:Option<&str> = None;
//pub const JAR_LAUNCHER_ARGS:&[(i32,&str)] = &[];
@RustParsable.Prefix("JAR_LAUNCHER_")
data class LauncherConfig(
    var file: String?=null,
    var mainClass: String? = null,
    var args: Map<Int, String> = emptyMap(),
):RustParsable

//pub const JRE_SEARCH_DIR:&[&str] = &["./lib/runtime"];
//pub const JRE_SEARCH_ENV:&[&str] = &["JAVA_HOME"];
//pub const JRE_OPTIONS:&[&str] = &[];
//pub const JRE_NATIVE_LIBS:&[&str] = &[];
//pub const JRE_VERSION:&[&str] = &["19.0"];
@RustParsable.Prefix("JRE_")
data class JreConfig(
    var searchDir: MutableSet<JvmSearch.JvmDir> = mutableSetOf(),
    var searchEnv : MutableSet<JvmSearch.EnvVar> = mutableSetOf(),
    val search: MutableSet<JvmSearch> = object : MutableSet<JvmSearch> by mutableSetOf(),RustParsable {
        override fun add(element: JvmSearch): Boolean { return when (element) {
                is JvmSearch.JvmDir -> searchDir.add(element)
                is JvmSearch.EnvVar -> searchEnv.add(element)
        } }
        override fun addAll(elements: Collection<JvmSearch>): Boolean = elements.all { add(it) }
        override fun remove(element: JvmSearch): Boolean { return when (element) {
            is JvmSearch.JvmDir -> searchDir.remove(element)
            is JvmSearch.EnvVar -> searchEnv.remove(element)
        } }
        override fun removeAll(elements: Collection<JvmSearch>): Boolean = elements.all { remove(it) }
        override fun retainAll(elements: Collection<JvmSearch>): Boolean = elements.all { remove(it) }
        override fun clear() { searchDir.clear();searchEnv.clear() }

    },
    var options: MutableSet<String> = mutableSetOf(),
    @RustParsable.Name("NATIVE_LIBS")
    var nativeLibsPath: MutableSet<String> = mutableSetOf(),
    @RustParsable.Type("&[&'static str]")
    var version: Set<Number> = emptySet(),
//    val preferred: JrePreferred = JrePreferred.DefaultVM
):RustParsable {
    infix fun versions(range: ClosedFloatingPointRange<Float>) {
        version = sequenceOf(1.6f,1.7f,1.8f,9f,10f,11f,12f,13f,14f,15f,16f,17f,18f,19f,20f,21f,22f,23f,24f)
            .filter { it in range }.toSet()
    }

    override fun parsingValueExtra(name: String): (() -> String)? {
        if (name == "version") return {
            version.map { f -> when (f) {
                is Double -> f.toInt().takeIf { it>2 } ?:f
                is Float -> f.toInt().takeIf { it>2 } ?:f
                else -> f
            }.toString()
            }.joinToString(",") {
                "\"$it\""
            }
        }
        return null
    }

}
//pub const CHARSET_STDOUT:Option<&'static str> = Some("GBK");
//pub const CHARSET_JVM:Option<&'static str> = None;
//pub const CHARSET_PAGE_CODE:Option<&'static str> = None;
@RustParsable.Prefix("CHARSET_")
data class CharsetConfig(
    var jvm:String? = null,
    var stdout :String? = null,
    var pageCode: String? = null
):RustParsable {
    companion object {
        val UTF8 get() = CharsetConfig(
            jvm = "UTF-8",
//            stdout = "UTF-8",
            pageCode = "65001"
        )
    }
}

//sealed class JrePreferred {
//    object DefaultVM : JrePreferred()
//    object ClientHotspot : JrePreferred()
//    object ServerHotspot : JrePreferred()
//}

//pub const SPLASH_SCREEN_IMAGE_PATH:Option<&'static str> = Some("H:\\tupm\\ic_ast_ugly.png");
//pub const SPLASH_SCREEN_IMAGE_FILE:Option<&'static str> = None;
@RustParsable.Prefix("SPLASH_SCREEN_")
data class SplashScreenConfig(
    var imagePath: String? = null,
//    var startLine: SplashScreenText? = null,
//    var versionLine: SplashScreenText? = null
):RustParsable

//data class SplashScreenText(
//    var text: String = "",
//    var fontName: String = "SansSerif",
//    var fontSize: Int = 12,
//    var fontStyle: Int = 0,
//    var color: Int = -1
//)
sealed interface JvmSearch {
    data class JvmDir(val path: String):JvmSearch
    data class EnvVar(val name: String):JvmSearch
}

