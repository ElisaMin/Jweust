package me.heizi.jweust.tasks

import me.heizi.jweust.JweustExtension
import me.heizi.jweust.JweustTasks
import me.heizi.jweust.JweustVarsExtension
import me.heizi.jweust.JweustVarsExtension.Companion.asJwConfig
import me.heizi.jweust.beans.JweustConfig
import me.heizi.jweust.beans.JweustDefault
import me.heizi.jweust.beans.allow
import me.heizi.jweust.beans.default
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject

open class TaskUpdateRepo @Inject constructor (
    extension: JweustExtension
): JweustTasks.TaskBase(extension) {

    companion object {
        const val NAME = "updateExeRepo"
    }
    init {
        group = "jweust"
        description = "clone, fetch and update RustProject for generating EXE, in '\${rootProject.buildDir}/tmp/jweust' by default. its not gonna clean and build"

    }

    val varKt: JweustConfig
        @Internal
        get() = asJwConfig
    @get:Internal
    val jarForInclude: File? by lazy {

        require(hashOfIncludeJar!= JweustVarsExtension.includeEnabledBut) {
            "hash jar failed, please report this issue to github."
        }

        hashOfIncludeJar?.trim()?.takeIf { it.isNotEmpty() } ?: return@lazy null

        jar.runCatching { files
            .toTypedArray()[launcher?.file?:0]
            .let(::File)
            .takeIf { it.exists() }!!
        }.onFailure {
            throw IllegalStateException("jar config is not correct,please set a correct jar file path in jweust block",it)
        }.getOrThrow()
    }

    override fun taskAction() {
        if (project.allow(JweustDefault.ALL)) with(project) {
            default()
        }
        generateValidatedRustProject()
    }


}