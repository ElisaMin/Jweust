package me.heizi.jweust.beans

import me.heizi.jweust.JweustPlugin
import me.heizi.jweust.jweust
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class JweustDefaultConfigTest {
    private val project by lazy { ProjectBuilder.builder()
        .withName(this::class.simpleName!!)
        .build().
    run {
        version = "1.1.0"
        group = this::class.java.`package`
        plugins.apply(JweustPlugin::class)
        plugins.apply("java")
        configure<JavaPluginExtension>{
            toolchain.languageVersion.set(JavaLanguageVersion.of(19))
        }
        this
    } }

    @Test fun `test jar`() {
        project
        println("config jar s")
        val jars = project.tasks.withType(Jar::class.java).toList().map {
            it.outputs.files.files.joinToString { it.name+"\n" }
        }
        assert(jars.isNotEmpty())
        println(jars.joinToString("\n"))
    }
    @Test fun `test default jar` () {
        with(project) {
            jweust {
                defaults()
                println(rustProjectName)
                println(jar.files)
                println(jre.version)
            }
        }

    }

}