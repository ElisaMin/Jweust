@file:Suppress("unused")
package me.heizi.jweust

import me.heizi.jweust.beans.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.extra
import java.io.File


class JweustPlugin: Plugin<Project> {
    override fun apply(project: Project) { with(project) {
        configurations.create(EXTENSION_NAME)
        extensions.add(JweustExtension::class.java, EXTENSION_NAME,JweustExtension(this))
        tasks.register(
            JweustTask.NAME,
            JweustTask::class.java,
            extensions.getByType(JweustExtension::class.java)
        )
    } }
    companion object {
        const val EXTENSION_NAME = "jweust"
    }
}

/**
 * jweust root configurer
 *
 * @property jweustRoot it will be the target of rust building after the project's clone to here
 */
interface JweustProjectExtension {

    /**
     * defined a root path to build the exe from jweust rust project and its same dir as clone tasks
     *
     * @see JweustTask.clone
     * @see JweustTask.cloneBefore
     */
    var jweustRoot:File
    @get:Input
    var rustProjectVersion:String
}

/**
 * unnecessary override
 *
 * @see JweustConfig
 */
interface JweustVarsExtension {
    @get:Input
    var rustProjectName:String
    @get:Input
    var applicationType: ApplicationType
    @get:Input
    var workdir: String?
    @get:Input
    var log: LogConfig
    @get:Input
    var exe: ExeConfig
    @get:Input
    var jar: JarConfig
    @get:Input
    var jre: JreConfig
    @get:Input
    var charset: CharsetConfig
    @get:Input
    var splashScreen: SplashScreenConfig?

    fun log(block: LogConfig.() -> Unit) {
        log = log.apply(block)
    }
    fun exe(block: ExeConfig.() -> Unit) {
        exe = exe.apply(block)
    }
    fun jar(block: JarConfig.() -> Unit) {
        jar = jar.apply(block)
    }
    fun jre(block: JreConfig.() -> Unit) {
        jre = jre.apply(block)
    }
    fun splashScreen(block: SplashScreenConfig.() -> Unit) {
        splashScreen = (splashScreen?: SplashScreenConfig()).apply(block)
    }
    fun charset(block:CharsetConfig.() -> Unit) {
        charset = charset.apply(block)
    }

}


/**
 * unnecessary override
 *
 * @see JweustProjectExtension
 * @see JweustConfig
 */
interface JweustExtension:JweustProjectExtension,JweustVarsExtension {
    companion object {
        @Suppress("NOTHING_TO_INLINE")
        internal inline operator fun invoke(project: Project):JweustExtension =
            object : JweustVarsExtension by JweustConfig(), JweustExtension {
                override var jweustRoot: File = project.rootDir.resolve("jweust")
                override var rustProjectVersion: String = project.version.toString().createValidatedVersionOf(3)
            }
    }
    fun Project.disableDefaults() {
        extra[JweustDefault.ALL] = false
    }
    fun Project.defaults(
        defaultName: Boolean = true,
        generateExeConfig: Boolean = true,
        generateJarConfig: Boolean = true,
        generateJreConfig: Boolean = true,
        generateRustProjectVersion: Boolean = true,
    ) {
        extra[JweustDefault.ALL] = true
        extra[JweustDefault.NAME] = defaultName
        extra[JweustDefault.EXE] = generateExeConfig
        extra[JweustDefault.JAR] = generateJarConfig
        extra[JweustDefault.JRE] = generateJreConfig
        extra[JweustDefault.VERSION] = generateRustProjectVersion
        default()
    }
}
