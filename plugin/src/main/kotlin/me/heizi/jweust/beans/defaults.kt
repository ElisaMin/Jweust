package me.heizi.jweust.beans

import me.heizi.jweust.JweustExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra


object JweustDefault {
    const val ALL = "jweust.default"
    const val JAR = "jweust.default.jar"
    const val EXE = "jweust.default.exe"
    const val JRE = "jweust.default.jre"
    const val NAME = "jweust.default.name"
    const val VERSION = "jweust.default.version"
}

fun ExtraPropertiesExtension.getOrNull(key:String) = runCatching { get(key) }.getOrNull()
fun Project.allow(key:String) = extra.getOrNull(key) != false
inline fun Project.doIfAllow(key:String, crossinline block:()->Unit) {
    if (allow(key)) block()
}

context(Project)
internal fun JweustExtension.default() = doIfAllow(JweustDefault.ALL) {
    doIfAllow(JweustDefault.NAME) {
        rustProjectName = name.toSnackCase().takeIf { it.isNotBlank() }!!
    }
    doIfAllow(JweustDefault.JAR) {
        jar.default()?.let { doIfAllow(JweustDefault.NAME) {
            rustProjectName = it
        } }
    }
    doIfAllow(JweustDefault.EXE) {
        exe.default()
    }
    doIfAllow(JweustDefault.JRE) {
        jre.default()
    }
    doIfAllow(JweustDefault.VERSION) {
        rustProjectVersion = version.toString().createValidatedVersionOf(3)
    }
    logger.info("Jweust: default settings is done")
    logger.debug("Jweust: default, {}", this)
}

context(Project)
@JvmName("defaultJarSettings")
internal fun JarConfig.default():String? {
    var jarName:String? = null
    fun foundByOutputFiles(name:String) =
        tasks.findByName(name)
            ?.outputs?.files?.firstOrNull()?.name
            ?.let {fileName ->
                files = setOf(fileName)
                jarName= fileName.removeSuffix(".jar")
            }
    if (files.isEmpty())
    foundByOutputFiles("shadowJar") ?:
    foundByOutputFiles("jar")
    logger.info("Jweust: JAR default settings is done")
    logger.debug("Jweust: jar default, {}", this)
    return jarName
}

context(Project)
@JvmName("defaultExeSettings")
internal fun ExeConfig.default() {
    group.toString().takeIf { it.isNotBlank() }?.let { owner ->
        if (companyName.isEmpty())
            companyName = owner
        if (legalCopyright.isEmpty())
            legalCopyright = owner
    }

    version.toString().takeIf { it.isNotBlank() }?.let { version->
        if (fileVersion.isEmpty()) fileVersion = version.createValidatedVersionOf(4)
        if (productVersion.isEmpty()) productVersion = version.createValidatedVersionOf(4)
        fileVersion = version
        productVersion = version
    }
    name.takeIf { it.isNotBlank() }?.let { name ->
        if (productName.isEmpty()) productName = name
        if (internalName.isEmpty()) internalName = name
    }
    description.takeIf { it.isNullOrBlank() }?.let {
        if(fileDescription.isNotEmpty()) fileDescription = it
    }
    logger.info("Jweust: EXE default settings is done")
    logger.debug("Jweust: exe default, {}", this)
}
context(Project)
@JvmName("defaultJreSettings")
internal fun JreConfig.default() {
    val logger = logger
    // lifecycle is after set
    if (searchEnv.isEmpty()) {
        logger.info("Jweust: JRE search env is empty, using default")
        searchEnv += JvmSearch.EnvVar("JAVA_HOME")
    }
    if (version.isEmpty()) {
        logger.info("Jweust: JRE version is empty, searching")
        val jvm = extensions.findByType(JavaPluginExtension::class.java)
            ?.toolchain
            ?.languageVersion
            ?.get()
            ?.asInt()
            ?:
            tasks.runCatching {
                withType(JavaCompile::class.java)
            }.getOrNull()?.let {
                it.firstOrNull()?.targetCompatibility?.split(".")?.last()
            }?.toIntOrNull()
            ?:
            11
        logger.info("Jweust: JRE version is $jvm")
        version += jvm
    }
}
