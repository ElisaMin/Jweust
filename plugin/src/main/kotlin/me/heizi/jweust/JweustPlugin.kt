@file:Suppress("unused")
package me.heizi.jweust

import org.gradle.api.Project
import org.gradle.api.Plugin
import java.io.File

@Suppress("FunctionName")
fun Project.Jweust(configure: JweustExtension.() -> Unit) {
    extensions.configure("jweust", configure)
}


/**
 * A simple 'hello world' plugin.
 */
class JweustPlugin: Plugin<Project> {
    private fun getConfig(project: Project): JweustExtension = object:JweustExtension,
        JweustProjectExtension,
        JweustVarsExtension by project.Jweust
    {
        override var jweustRoot: File = project.buildDir.resolve("jweust")
    }
    override fun apply(project: Project) { with(project) {
        extensions.add(JweustExtension::class.java,"jweust",getConfig(project))
        tasks.register(
            JweustTask.NAME,
            JweustTask::class.java,
            extensions.getByType(JweustExtension::class.java)
        )
    } }
}

interface JweustExtension:JweustProjectExtension,JweustVarsExtension

interface JweustProjectExtension {
    var jweustRoot:File
}
interface JweustVarsExtension {
    var rustProjectName:String
    var applicationType: ApplicationType
    var workdir: String
    var log: LogConfig
    var exe: ExeConfig
    var jar: JarConfig
    var jre: JreConfig
    var charset: CharsetConfig
    var splashScreen: SplashScreenConfig?

    fun log(block: LogConfig.() -> Unit) {
        log = LogConfig().apply(block)
    }
    fun exe(block: ExeConfig.() -> Unit) {
        exe = ExeConfig().apply(block)
    }
    fun jar(block: JarConfig.() -> Unit) {
        jar = JarConfig().apply(block)
    }
    fun jre(block: JreConfig.() -> Unit) {
        jre = JreConfig().apply(block)
    }
    fun splashScreen(block: SplashScreenConfig.() -> Unit) {
        splashScreen = SplashScreenConfig().apply(block)
    }
    fun charset(block:CharsetConfig.() -> Unit) {
        charset = CharsetConfig().apply(block)
    }

}
