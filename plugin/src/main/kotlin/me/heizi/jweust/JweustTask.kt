package me.heizi.jweust

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.heizi.kotlinx.shell.*
import org.gradle.api.DefaultTask
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject


open class JweustTask @Inject constructor (
    extension: JweustExtension
): DefaultTask(),JweustExtension by extension {
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
    internal fun clone(): CommandResult? = runBlocking {
        val reasonNotToClone = cloneBefore()
        if (reasonNotToClone!=null) {
            logger.warn(reasonNotToClone)
            return@runBlocking null
        } else {
            return@runBlocking Shell(
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
                outputs.files(jweustRoot.listFiles())
                    .withPropertyName("jweust.rust.files")
            }
        }
    }
    internal fun parse() = jweustRoot.absoluteFile.resolve(FILE_WITH_DIR,).run {
        FileWriter(this,false).use {
            it.write(rustConfig)
            it.flush()
        }
        val parsed = readText()
        require(parsed.lines().size>29) {
            "is not valid rust config"
        }
        outputs.file(this)
            .withPropertyName("jweust.rust.config")
        return@run parsed
    }
    private val config get() = JweustConfig(
        rustProjectName,applicationType,workdir,
        log,exe,jar,jre,charset,splashScreen
    )

    private val rustConfig
        get() = config.getRustFile()


    @OptIn(ExperimentalApiReShell::class)
    internal fun build() = runBlocking {
        ReShell("cargo build --release", workdir = jweustRoot)
            .map { it.also {
                when(it) {
                    is Signal.Output -> logger.info(it.message)
                    is Signal.Error -> logger.error(it.message)
                    is Signal.Code -> logger.info("Exit code: ${it.code}")
                    else -> Unit
                }
            } }
            .await()
    }
    init {
        group = "jweust"
        doFirst("clone") {
            clone()
        }
        doLast("parse") {
            parse()
        }
        doLast("build") {
            build()
        }
    }

}
