@file:Suppress("unused")
package me.heizi.jweust

import me.heizi.jweust.JweustVarsExtension.Companion.asJwConfig
import me.heizi.jweust.beans.*
import me.heizi.jweust.tasks.TaskCompileExe
import me.heizi.jweust.tasks.TaskJweust
import me.heizi.jweust.tasks.TaskUpdateRepo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.kotlin.dsl.embedJar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import java.io.File


class JweustPlugin: Plugin<Project> {

    private val tasksMap get() = mapOf(
        TaskUpdateRepo.NAME to TaskUpdateRepo::class.java,
        TaskCompileExe.NAME to TaskCompileExe::class.java,
        TaskJweust.NAME to TaskJweust::class.java,
    )

    private fun registering(project: Project) = with(project) {
        val extension = extensions.getByType<JweustExtension>()
        for( (name,clazz) in tasksMap) {
            tasks.register(name,clazz,extension).get()
        }
        afterEvaluate {
            val jar = with(tasks) { findByName("shadowJar")?:findByName("jar") }
            for( (name,clazz) in tasksMap) {
                tasks.named(name,clazz) {
                    if (embedJar) {
                        if (jar != null) dependsOn(jar)
                        else logger
                            .warn("Jweust: embedJar is true but jar task is not found")
                    }
                    if (name == TaskJweust.NAME) {
                        dependsOn(TaskUpdateRepo.NAME)
                        finalizedBy(TaskCompileExe.NAME)
//                        mustRunAfter(TaskCompileExe.NAME)
                    }
                }
            }
        }
    }
    override fun apply(project: Project) { with(project) {
        configurations.create(EXTENSION_NAME)
        extensions.add(JweustExtension::class.java, EXTENSION_NAME,JweustExtension(this))
        registering(project)
        artifacts.add(EXTENSION_NAME,buildDir.resolve("exe/${project.name}.exe")) {
            type = "exe"
        }
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
     * @see [me.heizi.jweust.tasks.generateValidatedRustProject]
     */
    @get:OutputDirectory
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
    /**
     * Embed jar into exe file, it's enabled by default.
     *
     * The hash will be generated at tasks runtime by reading the jar file, please specify an
     * **absolute path** .
     * @see [me.heizi.jweust.beans.JarConfig]
     * Set it as `Null` to disable embedding.
     */
    @get:Input
    @get:Optional
    var hashOfIncludeJar: String?
    @get:Input
    var rustProjectName:String
    @get:Input
    var applicationType: ApplicationType
    @get:Input
    @get:Optional
    var workdir: String?
    @get:Input
    var log: LogConfig
    @get:Nested
    var exe: ExeConfig
    @get:Nested
    var jar: JarConfig
    @get:Nested
    var jre: JreConfig
    @get:Nested
    var charset: CharsetConfig
    @get:Nested
    @get:Optional
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
    companion object {
        internal inline val JweustVarsExtension.asJwConfig get() = JweustConfig(hashOfIncludeJar,rustProjectName,applicationType,workdir, log,exe,jar,jre,charset,splashScreen)
        const val includeEnabledBut = "*"
    }
    fun includeJarById(hash:String) {
        hashOfIncludeJar = hash
    }
    fun includeJarByGenerate() {
        hashOfIncludeJar = includeEnabledBut
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
                override var jweustRoot: File = project.rootProject.buildDir.resolve("tmp/jweust")
                override var rustProjectVersion: String = project.version.toString().createValidatedVersionOf(3)
                override fun toString(): String = "root=$jweustRoot,version=$rustProjectVersion,${asJwConfig}"

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
