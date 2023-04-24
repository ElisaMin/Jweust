package me.heizi.jweust.beans

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

//pub const JAR_FILES:&[&str] = &[r#"./lib/AndroidPackageSideloadTool.jar"#];
@RustParsable.Prefix("JAR_")
data class JarConfig(
    @get:Input
    var files: Set<String> = emptySet(),
    @get:Input
    @get:Optional
    var launcher: LauncherConfig? = null
): RustParsable


//pub const JAR_LAUNCHER_FILE:usize = 0;
//pub const JAR_LAUNCHER_MAIN_CLASS:Option<&str> = None;
//pub const JAR_LAUNCHER_ARGS:&[(i32,&str)] = &[];
@RustParsable.Prefix("JAR_LAUNCHER_")
data class LauncherConfig(
    @RustParsable.Type("usize")
    var file: Int = 0,
    var mainClass: String? = null,
    var args: Map<Int, String> = emptyMap(),
):RustParsable {
    override fun parsingValueExtra(name: String): (() -> String)?
            = if (name == "file") { {
        file.toString()
    } } else null
}