package me.heizi.jweust.beans

import me.heizi.jweust.JweustExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra

context(Project)
internal fun JweustExtension.default() {
    if (extra["jweust.default.jar"] != false)
    rustProjectName = jar.default()
        ?: name.toSnackCase().takeIf { it.isNotBlank() }!!
    if (extra["jweust.default.exe"] != false)
    exe.default()
    if (extra["jweust.default.jre"] != false)
    jre.default()
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
        if (fileVersion.isEmpty()) fileVersion = version
        if (productVersion.isEmpty()) productVersion = version
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
