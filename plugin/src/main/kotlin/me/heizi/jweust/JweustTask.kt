package me.heizi.jweust

import me.heizi.jweust.JweustVarsExtension.Companion.asJwConfig
import me.heizi.jweust.beans.JweustDefault
import me.heizi.jweust.beans.allow
import me.heizi.jweust.beans.default
import me.heizi.jweust.beans.getRustFile
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Internal
import java.io.File
import javax.inject.Inject


open class JweustTask @Inject constructor (
    extension: JweustExtension
): DefaultTask(),JweustExtension by extension,JweustTasks {


    override val _logger: Logger
        @Internal
        get() = super.getLogger()
    override val rustConfig: String
        @Internal
        get() = asJwConfig.getRustFile()
    override val jarForInclude: File? get() =
        hashOfIncludeJar?.trim()?.takeIf {
            it.isNotEmpty() && it != "@|/dev/jweust_enable_jar_include_but_later_init"
        }?.let { _ -> with(jar) {
            launcher?.file?.let {
                files.toTypedArray().getOrNull(it)
            }?.let(::File)
        }} // 很没必要的写法

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
        doFirst("clone") {
            clone(this)
        }
        doLast("parse") {
            parse(this)
        }
        doLast("build") {
            build(this)
        }

    }

}
