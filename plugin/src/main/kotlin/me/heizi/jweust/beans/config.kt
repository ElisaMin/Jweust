@file:Suppress("unused")
package me.heizi.jweust.beans

import me.heizi.jweust.JweustVarsExtension


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


//pub const SPLASH_SCREEN_IMAGE_PATH:Option<&'static str> = Some("H:\\tupm\\ic_ast_ugly.png");
//pub const SPLASH_SCREEN_IMAGE_FILE:Option<&'static str> = None;
@RustParsable.Prefix("SPLASH_SCREEN_")
data class SplashScreenConfig(
    var imagePath: String? = null,
):RustParsable

sealed interface JvmSearch {
    data class JvmDir(val path: String):JvmSearch
    data class EnvVar(val name: String):JvmSearch
}


data class JweustConfig(
//    var includeJar: Boolean = false,
    override var rustProjectName:String = "",
    // pub const APPLICATION_WITH_OUT_CLI:Option<Option<&'static str>> = Some(Some("-DConsolog=true"));
    @RustParsable.Name("APPLICATION_WITH_OUT_CLI")
    @RustParsable.Type("Option<Option<&'static str>>")
    override var applicationType: ApplicationType = ApplicationType.ConsoleWhileOptionApplication(),
    override var workdir: String? = null,
    override var log: LogConfig = LogConfig(),
    override var exe: ExeConfig = ExeConfig(),
    override var jar: JarConfig = JarConfig(),
    override var jre: JreConfig = JreConfig(),
    override var charset: CharsetConfig = CharsetConfig(),
    override var splashScreen: SplashScreenConfig? = SplashScreenConfig(),
):RustParsable, JweustVarsExtension {
    override fun parsingValueExtra(name: String): (() -> String)? {
        if (name == "applicationType") return {
            applicationType.getRustValue()
        }
        return super.parsingValueExtra(name)
    }
}

