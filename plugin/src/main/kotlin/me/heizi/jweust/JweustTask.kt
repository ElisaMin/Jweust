package me.heizi.jweust

import me.heizi.jweust.JweustVarsExtension.Companion.asJwConfig
import me.heizi.jweust.JweustVarsExtension.Companion.includeEnabledBut
import me.heizi.jweust.beans.*
import me.heizi.jweust.tasks.generateValidatedRustProject
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.extra
import java.io.File
import javax.inject.Inject


open class JweustTask @Inject constructor (
    extension: JweustExtension
): DefaultTask(),JweustExtension by extension,JweustTasks {


    override val _logger: Logger
        @Internal
        get() = super.getLogger()
    override val varKt: JweustConfig
        @Internal
        get() = asJwConfig
    override val jarForInclude: File? @Internal get() {
        // null check or embedding is enabled
        hashOfIncludeJar
            ?.trim()
            ?.takeIf { with(it) {
                isNotEmpty() &&
                !equals(includeEnabledBut)
            } }
            ?: return null

        return jar.launcher?.file // index ?
            ?.let { index -> jar.files.toTypedArray().getOrNull(index) }
            ?.let(::File)
    }

    override fun getExtra(key: String): Any? {
        return project.extra.getOrNull(key)
    }

    companion object {
        const val NAME = "jweust"
    }

    internal fun taskAction() {
        doFirst {
            if (project.allow(JweustDefault.ALL))
                with(project) {
                    default()
                }
        }
        doFirst("update") {
            generateValidatedRustProject()
        }
        doLast("build") {
            build(this)
        }

    }

}
