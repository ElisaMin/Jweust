package me.heizi.jweust.beans

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional


//pub const LOG_STDERR_PATH:Option<(Option<&'static str>,bool)> = None;
//pub const LOG_STDOUT_PATH:Option<(Option<&'static str>,bool)> = None;
@RustParsable.Prefix("LOG_")
data class LogConfig(

    @RustParsable.Name("STDERR_PATH")
    @RustParsable.Type("Option<(Option<&'static str>,bool)>")
    @get:Input
    @get:Optional
    var error: LogFileConfig? = null,

    @RustParsable.Name("STDOUT_PATH")
    @RustParsable.Type("Option<(Option<&'static str>,bool)>")
    @get:Input
    @get:Optional
    var stdout: LogFileConfig? = null

): RustParsable {
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
