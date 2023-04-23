@file:Suppress("unused")
package me.heizi.jweust

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import java.io.File

class JweustPlugin: Plugin<Project> {
    override fun apply(project: Project) { with(project) {
        extensions.add(JweustExtension::class.java,"jweust",JweustExtension(this))
        tasks.register(
            JweustTask.NAME,
            JweustTask::class.java,
            extensions.getByType(JweustExtension::class.java)
        )
    } }
}
private val Project.jweustConfig get() = JweustConfig().apply {
    rustProjectName = name.toSnackCase()
    jre {
        search += JvmSearch.EnvVar("JAVA_HOME")
        extensions.findByType(JavaPluginExtension::class.java)
            ?.toolchain
            ?.languageVersion
            ?.get()
            ?.asInt()
            ?.let { version +=
                if (it>8) it else "1.${it}".toFloat()
            }
    }

    jar {
        fun foundByOutputFiles(name:String) =
            tasks.findByName(name)
                ?.outputs?.files?.firstOrNull()?.name
                ?.let {fileName ->
                    files = setOf(fileName)
                    rustProjectName= fileName.removeSuffix(".jar")
                }
        foundByOutputFiles("shadowJar") ?:
        foundByOutputFiles("jar")
        Runtime.version().let {
            jre.version += it.feature()
        }
    }

    exe {
        group.toString().let { owner ->
            companyName = owner
            legalCopyright = owner
        }
        version.toString().let {version->
            fileVersion = version
            productVersion = version
        }
        name.let {
            productName = it
            internalName = it
        }

        fileDescription = description?:""
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
            object : JweustVarsExtension by project.jweustConfig, JweustExtension {
                override var jweustRoot: File = project.rootDir.resolve("jweust")
            }
    }
}
