package me.heizi.jweust

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class DemoGradleProjectTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy {
        projectDir.resolve("build.gradle.kts")
    }

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    private fun initProject() {

        settingsFile.writeText("")

        buildFile.writeText("""
            plugins {
                id("me.heizi.jweust")
                `java`
            }
            group = "me.heizi.jweust"
            version = "1.0"
            jweust {
                defaults()
                includeJarByGenerate()
                jar {
                    mainClass = "Main"
                }
            }
        """.trimIndent())

        projectDir.resolve("src/main/java/")
            .also { it.mkdirs() }
            .resolve("Main.java").
        writeText(
            """
            |public class Main {
            |    public static void main(String[] args) {
            |        System.out.println("Hello World!");
            |        try {
            |            System.out.println("Sleeping 5s");
            |            Thread.sleep(5000);
            |        } catch (InterruptedException e) {
            |            e.printStackTrace();
            |        }
            |    }
            |}
        """.trimMargin())
    }

    @Test fun `demo test`() {
        // Set up the test build
        initProject()
        // Run the build
        GradleRunner.create().
        withPluginClasspath().
        withArguments("jweust","--stacktrace","--info",).
        withProjectDir(projectDir).
        forwardOutput().
        build()
        val ext = projectDir.resolve("build/exe/").listFiles()!!.find { it.name.endsWith(".exe") }!!
        println(ext)
        // println size of exe in mb
        println("exe.size ="+ ext.length().toDouble() / 1024 + "kb")
        val jar = projectDir.resolve("build/libs/").listFiles()!!.find { it.name.endsWith(".jar") }!!
        println("jar.size ="+ jar.length().toDouble() / 1024 + "kb")
        println("size.dst ="+ (ext.length()-jar.length()).toDouble() / 1024 + "kb")


        // Run the exe
        println("Running exe, it will take 5s cuzing by Thread.sleep(5000) ")
        ProcessBuilder(ext.absolutePath)
            .start().run {
                val r = inputStream.bufferedReader().readText()
                waitFor()
                println(r)
                assert(r.contains("Hello World!") && r.contains("Sleeping 5s"))
            }
    }
}
