package me.heizi.jweust.tasks

import me.heizi.jweust.JweustTasks
import me.heizi.jweust.JweustVarsExtension
import me.heizi.jweust.beans.*
import java.io.File

/**
 * Update files
 *
 * Return true if any file is changed
 */
internal fun TaskUpdateRepo.updateFiles() =
    arrayOf(
        updateOrCreateVarRs(),
        updateIncludeRs(),
        updateToml()
    ).run {
        true in this
    }

internal fun JweustTasks.ignoreRemoveVarRs()
        = changeOrWrite(".gitignore") {
    lines().toMutableList().takeIf {
        it.removeIf { line -> line == "src/var.rs" }
    }?.joinToString("\n")
}

internal fun TaskUpdateRepo.updateIncludeRs()
= if (jarForInclude==null) false else
changeOrWrite("src/includes.rs") {
    asInclude(jarForInclude!!)
}
internal fun TaskUpdateRepo.updateOrCreateVarRs()
= changeOrWrite("src/var.rs") {
    if (hashOfIncludeJar == JweustVarsExtension.includeEnabledBut) {
        fun sha256(file: File):String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val fis = file.inputStream()
            val buffer = ByteArray(1024)
            var numRead = fis.read(buffer)
            while (numRead != -1) {
                digest.update(buffer, 0, numRead)
                numRead = fis.read(buffer)
            }
            fis.close()
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        val jarF = jar.files.toTypedArray()
            .getOrNull(jar.launcher?.file?:0)
            ?.let(::File)
            ?.takeIf { it.exists() }
        hashOfIncludeJar = if (jarF != null) sha256(jarF).also {
            _logger.info("hash of jar is $it")
        } else {
            _logger.warn("jar embedding is enabling but launcher jar not found in ${jar}, disabling jar embedding")
            null
        }
    }
    varKt.getRustFile().takeIf { it != this }
}
internal fun TaskUpdateRepo.updateToml(): Boolean
= changeOrWrite("Cargo.toml") {
    setCargoVars(
        projectName = rustProjectName,
        version = rustProjectVersion
    )
}
private fun String.asInclude(
    file:File
):String? {
    var lineForReplace = buildString {
        append("        ") // yes. tabs.
        append("include_bytes!(")
        append('"')
        append(file.absolutePath.replace("\\", "\\\\"))
        append('"')
        append(")")
    }
    if (lineForReplace in this) return null

    val lines = lines()
    var writingSwitch: Boolean? = false

    return buildString { for (line in lines) {

        if (line.endsWith("//jweust-include-jar-start")) {
            writingSwitch = true
            appendLine(line)
            continue
        }

        if (line.endsWith("//jweust-include-jar-end")) {
            writingSwitch = false
        }

        if (writingSwitch==true) {
            appendLine(lineForReplace)
            lineForReplace = "        "
        } else {
            appendLine(line)
        }

    } }.takeIf { it!=this }

}


@Suppress("NAME_SHADOWING")
private fun String.setCargoVars(
    projectName:String,
    version:String,
):String? {
    val lines = lines().toMutableList()

    val projectName = lines.lineDiff(1,"name",projectName)
    val version = lines.lineDiff(2,"version",version)

    if (projectName==null && version==null) return null

    projectName?.let { lines[1] = it }
    version?.let { lines[2] = it }

    return lines.joinToString("\n")
}


private fun List<String>.lineDiff(index: Int, name: String, to:String): String? {
    val regex = Regex("$name = \"(.+)\"")
    val oLine = this[index]
    return this[index].replace(regex){
        "$name = \"$to\""
    }.takeIf { it!=oLine }
}

internal fun JweustConfig.getRustFile():String {
    workdir = workdir?.takeIf { it.isNotBlank() }
    exe {
        productVersion = productVersion.createValidatedVersionOf(4)
        fileVersion = fileVersion.createValidatedVersionOf(4)
    }
    return arrayOf(
        this,
        log,
        exe,
        jar, jar.launcher ?: LauncherConfig(),
        jre,
        charset,
        splashScreen?: SplashScreenConfig(),
    )
        .asSequence()
        .map { it.parsePartOfFile() }
        .flatMap { it.lines() }
        .filter { it.isNotEmpty() }
        .joinToString("\n", prefix = "#![allow(dead_code)]\n")
}



private inline fun JweustTasks.changeOrWrite(
    path:String,
    crossinline block:String.()->String?
):Boolean {
    val file = jweustRoot.resolve(path)
    val isVar = path == "src/var.rs"
    if (isVar) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }
    require(file.exists()) {
        "file $file not found"
    }
    file.readText()
        .also {
            if (!isVar && it.isBlank()) {
                _logger.warn("file $file is empty")
            }
        }
        .block()
        ?.let { file.writeText(it) }
        ?: return false
    return true
}