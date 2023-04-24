package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.jweust.beans.JweustConfig
import me.heizi.jweust.beans.default
import me.heizi.jweust.beans.getRustFile
import me.heizi.kotlinx.shell.*
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

@Suppress("LeakingThis")
open class JweustTask @Inject constructor (
    extension: JweustExtension
): DefaultTask(),JweustExtension by extension {

    private fun Task.save(vararg files: File, property: String?=null) {
        this.outputs.file(files.clone()).run {
            if (property!=null) withPropertyName("jweust.$property")
        }
    }
    companion object {
        const val NAME = "jweust"
        private const val FILE_WITH_DIR = "src/var.rs"
    }
    private fun cloneBefore():String?{
        if (jweustRoot.exists()) {
            if (jweustRoot.isFile) throw IllegalStateException("Jweust root is a file")
            if (jweustRoot.listFiles()?.isNotEmpty() == true) {
                return "jweust ain't empty. will not to be clone"
            }
        }
        jweustRoot.deleteOnExit()
        return null
    }
    internal fun clone():CommandResult?  {
        val reasonNotToClone = cloneBefore()
        return if (reasonNotToClone!=null) {
            logger.warn(reasonNotToClone)
            null
        } else runBlocking {
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
    }

    private inline val config get() = JweustConfig(rustProjectName,applicationType,workdir, log,exe,jar,jre,charset,splashScreen)
    private inline val rustConfig get() = config.getRustFile()

    internal fun parse() = jweustRoot.absoluteFile.resolve(FILE_WITH_DIR).run {
        FileWriter(this,false).use {
            it.write(rustConfig)
            it.flush()
        }
        val parsed = readText()
        require(parsed.lines().size>29) {
            "is not valid rust config\n$parsed"
        }
        return@run parsed to arrayOf(this,parseAfter())
    }

    // replace the version and name in cargo.toml
    private fun parseAfter() = jweustRoot.resolve("cargo.toml").apply {
        val o = readText().lines()
        val (name,version) = o.run {
            lineDiff(1,"name",rustProjectName) to lineDiff(2,"version",exe.productVersion.split('.').run {
                when {
                    last()=="0" -> dropLast(1)
                    first()=="0" -> drop(1)
                    else -> dropLast(1)
                }
            }.joinToString("."))
        }
        if (name!=null || version!=null ) o.toMutableList().apply {
            name?.let { this[1] = it }
            version?.let { this[2] = it }
        }.joinToString("\n").let { s->
            FileWriter(this,false).use {
                it.write(s)
                it.flush()
            }
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

    @OptIn(ExperimentalApiReShell::class)
    internal fun build() = runBlocking {
        ReShell("cargo build --release",
            workdir = jweustRoot,
            environment = mapOf("`RUST_BACKTRACE`" to "1")
        ).map { it.also {
            when(it) {
                is Signal.Output -> logger.info(it.message)
                is Signal.Error -> logger.error(it.message)
                is Signal.Code -> logger.info("Exit code: ${it.code}")
                else -> Unit
            }
        } }.await().apply {
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
        } as CommandResult.Success // as success
    } to afterBuild()
    // exe in /target/release/deps/${rustProjectName}.exe
    private fun afterBuild() = jweustRoot.resolve(
        "target/release/deps/"
    ).let { dir->
        require(dir.exists()&&dir.isDirectory) { "build failed" }
        dir.resolve("$rustProjectName.exe").takeIf { it.exists() } ?:
        dir.listFiles()?.first { it.name.endsWith(".exe") }
            ?: throw IllegalStateException("exe not found")
    }.apply {
        require(exists()&&isFile) {
            "$this is not a file"
        }
        val exe = this
        project.buildDir.resolve("jweust/").run {
            mkdirs()
            require(exists() && isDirectory) {
                "$this is not a directory"
            }
            exe.copyTo(this.resolve(exe.name),true).run {
                println("copy to $this")
                require(exists()&&isFile) {
                    "$this is not a file"
                }
            }
        }
        project.artifacts.add(JweustPlugin.EXTENSION_NAME,exe) {
            type = "exe"
        }
    }


    init {
        group = "jweust"
        if (project.extra["jweust.default"] != false) doFirst {
            with(project) {
                default()
            }
        }
        doFirst("clone") {
            clone()?.let {_->
                save(*jweustRoot.listFiles()!!.filter { file ->
                    val fileName = file.name
                    arrayOf(".git","cargo.toml") // exclude
                        .none { it == fileName }
                }.toTypedArray(),property = "rust.files")
            }
        }
        doLast("parse") {
            val (_,file) = parse()
            save(*file,property = "rust.config")
        }
        doLast("build") {
            val (_,file) = build()
            save(file,property = "rust.exe")
        }
    }

}
