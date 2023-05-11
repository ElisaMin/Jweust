package me.heizi.jweust.tasks

import me.heizi.jweust.JweustExtension
import me.heizi.jweust.JweustTasks
import org.gradle.api.Project
import java.io.File
import javax.inject.Inject

open class TaskCompileExe @Inject constructor(
    extension: JweustExtension
): JweustTasks.TaskBase(extension) {

    companion object {
        const val NAME = "compileExe"
    }

    init {
        project.tasks.run {
            (findByName("shadowJar")?:findByName("jar"))?.let {
                dependsOn(it)
            }
        }
        group = "jweust"
        description = "compile RustProject for generating EXE, in '\${rootProject.buildDir}/tmp/jweust' by default."
    }
    override fun taskAction() = with(project) {
        compile()
        buildDir.resolve("exe/").runCatching {
            if (!isDirectory)
                mkdirs()
            val exe = exeFile
            exe.copyTo(resolve(exe.name),true)
            exe.deleteOnExit()
        }
        Unit
    }
    private fun Project.compile() {
        exec {
            workingDir = jweustRoot
            environment["RUST_BACKTRACE"] = "1"
            commandLine("cargo","build","--release")
        }.rethrowFailure()
    }
    private val exeFile:File get()  {
        val exes = jweustRoot.resolve("target/release/deps/")
            .listFiles()?.filter {
                it.name.startsWith(rustProjectName) && it.name.endsWith(".exe")
            } ?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Can't find any exe file")
        if (exes.size == 1) return exes.first()
        return with(exes) {
            firstOrNull { it.nameWithoutExtension == rustProjectName }
                ?: firstOrNull { it.nameWithoutExtension == rustProjectName.replace("-","_") }
                ?: first()
        }
    }

}