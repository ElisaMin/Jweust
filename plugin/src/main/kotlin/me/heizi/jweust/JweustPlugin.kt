@file:Suppress("unused")
package me.heizi.jweust

import me.heizi.jweust.beans.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Input
import java.io.File
import java.net.URI


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
 * ~I think it should generate by gradle kts~del-line
 */
@Suppress("FunctionName")
fun Project.Jweust(configure: JweustExtension.() -> Unit) {
    extensions.configure("jweust", configure)
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

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(project: Project) = object : JweustProjectExtension {
            override var jweustRoot: File = project.rootDir.resolve("jweust")
        }
    }
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
            }
    }
}
@Deprecated("its not usually method, it will be removed in future", level =  DeprecationLevel.HIDDEN,
    // GitHub Packages
    replaceWith = ReplaceWith("maven { url = URI.create(\"https://maven.pkg.github.com/ElisaMin/Khell\") }")
)
fun RepositoryHandler.heiziGithubRepo() {
    maven {
        url = URI.create("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
    }
}
