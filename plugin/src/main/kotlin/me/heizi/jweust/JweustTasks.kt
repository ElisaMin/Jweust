package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.jweust.beans.JweustConfig
import me.heizi.jweust.tasks.updateFiles
import me.heizi.kotlinx.shell.*
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException


internal interface JweustTasks: JweustProjectExtension {

    val varKt:JweustConfig
    @Suppress("PropertyName")
    val _logger: Logger
    val rustProjectName:String
    val jarForInclude:File?
    fun getExtra(key:String):Any?

    fun clone(task: Task) {
        checkCloneDirsWithReason()?.let {
            _logger.warn(it)
            return
        }
        justClone()
        _logger.lifecycle("clone is done")
    }

    fun parse():Boolean {
        return updateFiles()
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
}
