package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.kotlinx.shell.*
import org.gradle.api.Task
import org.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException


// for test
@Suppress("NOTHING_TO_INLINE")
internal inline fun JweustTasks.cloneWithOutSave()  {
    checkCloneDirsWithReason()?.let {
        logger.warn(it)
        return
    }
    justClone()
}
@Suppress("NOTHING_TO_INLINE")
internal inline fun JweustTasks.parseWithOutSave() =
    arrayOf(parseVars(),parseToml())
@Suppress("NOTHING_TO_INLINE")
internal inline fun JweustTasks.buildWithOutSave(): File {
    buildRust()
    return searchExe()
}

//
internal interface JweustTasks: JweustProjectExtension {

    val rustConfig: String
    val logger: Logger
    val rustProjectName:String

    fun clone(task: Task) {
        cloneWithOutSave()
        with(task) {
            saveCloneResult()
        }
    }

    fun parse(task: Task) {
        val parsed = parseWithOutSave()
        with(task) {
            saveParseResult(parsed)
        }
    }

    fun build(task: Task) {
        buildWithOutSave()
        with(task) { saveBuildResult(
            setArtifact(searchExe())
        ) }
    }

    fun clean() {
        jweustRoot.deleteRecursively()
    }
    private fun Task.save(vararg files: File, property: String?=null) {
        this.outputs.file(files.clone()).run {
            if (property!=null) withPropertyName("jweust.$property")
        }
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


    context(Task)
    fun saveCloneResult() {
        save(*jweustRoot.listFiles()!!.filter { file ->
            val fileName = file.name
            arrayOf(".git","cargo.toml") // exclude
                .none { it == fileName }
        }.toTypedArray(),property = "rust.files")
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
    context(Task)
    fun saveParseResult(parsed:Array<File>) {
        save(*parsed,property = "rust.parsed")
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
                    is Signal.Output -> logger.info(it.message)
                    is Signal.Error -> logger.error(it.message)
                    is Signal.Code -> logger.info("Exit code: ${it.code}")
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
        logger.info("build success. exe : $absolutePath")
    }
    context(Task)
    fun setArtifact(artifact: File): File {
        val r = project.buildDir.resolve("jweust/").runCatching {
            mkdirs()
            artifact.copyTo(this.resolve(artifact.name),true)
        }.onSuccess {
            artifact.deleteOnExit()
        }.getOrDefault(artifact)
        project.artifacts.add(JweustPlugin.EXTENSION_NAME,r) {
            type = "exe"
        }
        return r
    }

    context(Task)
    fun saveBuildResult(exe: File) {
        save(exe,property = "rust.exe")
    }


    companion object {
        private const val FILE_WITH_DIR = "src/var.rs"
    }
}
