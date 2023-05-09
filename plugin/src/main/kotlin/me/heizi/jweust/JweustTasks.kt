package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.kotlinx.shell.*
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException


internal interface JweustTasks: JweustProjectExtension {

    val rustConfig: String
    @Suppress("PropertyName")
    val _logger: Logger
    val rustProjectName:String
    val jarForInclude:File?

    fun clone(task: Task) {
        checkCloneDirsWithReason()?.let {
            _logger.warn(it)
            return
        }
        justClone()
        _logger.lifecycle("clone is done")
    }

    fun parse(task: Task) {
        parseVars()
        parseToml()
        if (jarForInclude != null)
            parseInclude()
        _logger.lifecycle("parse is done")
    }

    fun build(task: Task) {
        buildRust()
        with(task) {
            setArtifact(searchExe())
        }
        _logger.lifecycle("build is done")
    }

    fun clean(task: Task) {
        jweustRoot.deleteRecursively()
    }

    fun checkCloneDirsWithReason():String? {
        if (jweustRoot.exists()) {
            if (jweustRoot.isFile) throw IllegalStateException("Jweust root is a file")
            if (jweustRoot.listFiles()?.isNotEmpty() == true) {
                return "jweust ain't empty. will not to be clone"
            }
        }
        jweustRoot.deleteOnExit()
        return null
    }
    fun justClone() = runBlocking {
        Shell(
            "git clone git@github.com:ElisaMin/Jweust-template.git ${jweustRoot.absolutePath}"
        ).await().apply {
            require(this is CommandResult.Success) {
                (this as CommandResult.Failed).let {
                    """|${it.processingMessage}
                            |${it.errorMessage}
                            |${it.code}
                        """.trimMargin()
                }.let { IOException(it) }
            }
        }
    }


    fun parseVars() = jweustRoot.absoluteFile.resolve(FILE_WITH_DIR).apply {
        FileWriter(this, false).use {
            it.write(rustConfig)
            it.flush()
        }
        val parsed = readText()
        require(parsed.lines().size>29) {
            "is not valid rust config\n$parsed"
        }
    }
    fun parseInclude() = jweustRoot.absoluteFile.resolve("src/includes").apply {

        var lineForReplace = buildString {
            append("        ") // yes. tabs.
            append("include_bytes!(")
            append('"')
            append(jarForInclude!!.absolutePath.replace("\\", "\\\\"))
            append('"')
            append(");")
        }

         val content = buildString {

             var writingSwitch = false

             readLines().forEach { with(it) {

                 if (endsWith("//jweust-include-jar-start")) {
                     writingSwitch = true
                 }
                 if (endsWith("//jweust-include-jar-end")) {
                     writingSwitch = false
                 }
                 if (writingSwitch) {
                     appendLine(lineForReplace)
                     lineForReplace = "        "
                 } else {
                     appendLine(this)
                 }
             } }

        }

        FileWriter(this, false).use {
            it.write(content)
            it.flush()
        }
    }
    @Suppress("NOTHING_TO_INLINE")
    private inline fun List<String>.lineDiff(index: Int, name: String, to:String): String? {
        val regex = Regex("$name = \"(.+)\"")
        val oLine = this[index]
        return this[index].replace(regex){
            "$name = \"$to\""
        }.takeIf { it!=oLine }
    }
    fun parseToml() = jweustRoot.absoluteFile.resolve("Cargo.toml").apply {
        val o = readText().lines()
        val (name,version) = o.run {
            lineDiff(1,"name",rustProjectName) to lineDiff(2,"version",rustProjectVersion)
        }
        if (name!=null || version!=null ) o.toMutableList().apply {
            name?.let { this[1] = it }
            version?.let { this[2] = it }
        }.joinToString("\n").let { s->
            FileWriter(this, false).use {
                it.write(s)
                it.flush()
            }
        }
    }
    @OptIn(ExperimentalApiReShell::class)
    fun buildRust() = runBlocking {
        ReShell(
            "cargo build --release",
            workdir = jweustRoot,
            environment = mapOf("`RUST_BACKTRACE`" to "1")
        ).map {
            it.also {
                when (it) {
                    is Signal.Output -> _logger.info(it.message)
                    is Signal.Error -> _logger.error(it.message)
                    is Signal.Code -> _logger.info("Exit code: ${it.code}")
                    else -> Unit
                }
            }
        }.await().apply {
            require(this is CommandResult.Success) {
                (this as CommandResult.Failed).let {
                    """
                        | Error: build tasks is failed
                        | - stdout
                        |${it.processingMessage}
                        | - stderr
                        |${it.errorMessage}
                        | - exit code ${it.code}
                    """.trimMargin()

                }.let { IOException(it) }
            }
        } as CommandResult.Success // as a success
    }
    fun searchExe() = jweustRoot.resolve("target/release/deps/").
    let { dir->
        require(dir.exists()&&dir.isDirectory) {
            "build failed"
        }
        dir.resolve("$rustProjectName.exe")
            .takeIf { it.exists() } ?:
        dir.listFiles()
            ?.first {
                it.name.endsWith(".exe")
            }
        ?: throw IllegalStateException("exe not found")
    }.apply {
        _logger.info("build success. exe : $absolutePath")
    }
    context(Task)
    fun setArtifact(artifact: File): File {
        val r = project.buildDir.resolve("jweust/").runCatching {
            mkdirs()
            artifact.copyTo(this.resolve(artifact.name),true)
        }.onSuccess {
            artifact.deleteOnExit()
        }.getOrDefault(artifact)
        return r
    }


    companion object {
        private const val FILE_WITH_DIR = "src/var.rs"
    }
}
