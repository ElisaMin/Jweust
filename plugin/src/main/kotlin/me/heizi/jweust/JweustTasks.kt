package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.jweust.beans.JweustConfig
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

    fun build(task: Task) {
        _logger.lifecycle("> Task :jweust:build: start")
        buildRust()
        with(task) {
            setArtifact(searchExe())
        }
        _logger.lifecycle("build is done")
    }

    @OptIn(ExperimentalApiReShell::class)
    fun buildRust() = runBlocking {
        _logger.lifecycle("> Task :jweust:build: building")
        ReShell(
            "cargo build --release",
            workdir = jweustRoot,
            environment = mapOf("`RUST_BACKTRACE`" to "1")
        ).map { it.also {
            when (it) {
                is Signal.Output -> _logger.lifecycle("> Task :jweust:build: ${it.message}")
                is Signal.Error -> _logger.warn("> ${it.message}")
                is Signal.Code -> _logger.lifecycle("> Exit code: ${it.code}")
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
        } as CommandResult.Success // as a success
    }

    fun searchExe() = jweustRoot.resolve("target/release/deps/").
        listFiles()?.filter {
            it.name.endsWith(".exe")
        }?.run {
            firstOrNull { it.nameWithoutExtension == rustProjectName }
                ?: firstOrNull { it.nameWithoutExtension == rustProjectName.replace("-","_") }
                ?: first()
        }?.apply {
            _logger.info("build success. exe : $absolutePath")
        } ?: throw IllegalStateException("exe not found")

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
