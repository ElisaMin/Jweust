package me.heizi.jweust

import kotlinx.coroutines.runBlocking
import me.heizi.kotlinx.shell.ExperimentalApiReShell
import me.heizi.kotlinx.shell.ReShell
import me.heizi.kotlinx.shell.Shell
import me.heizi.kotlinx.shell.await
import org.gradle.api.DefaultTask
import java.io.FileWriter
import javax.inject.Inject


open class JweustTask @Inject constructor (
    extension: JweustExtension
): DefaultTask(),JweustExtension by extension {
    companion object {
        const val NAME = "jweust"
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
    private fun clone() = runBlocking {
        val reasonNotToClone = cloneBefore()
        reasonNotToClone?.let {
            logger.warn(it)
        } ?: Shell(
            "git clone git@github.com:ElisaMin/Jweust-template.git ${jweustRoot.absolutePath}"
        ).await()
    }
    private fun parse() = jweustRoot.absoluteFile.resolve("src/vars.rs",).run {
        FileWriter(this,false).use {
            it.write(rustConfig)
            it.flush()
        }
    }
    private val config get() = JweustConfig(
        rustProjectName,applicationType,workdir,
        log,exe,jar,jre,charset,splashScreen
    )
    private val rustConfig get() = config.parse()

    @OptIn(ExperimentalApiReShell::class)
    private fun build() = runBlocking {
        ReShell("cargo build --release", workdir = jweustRoot)
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
