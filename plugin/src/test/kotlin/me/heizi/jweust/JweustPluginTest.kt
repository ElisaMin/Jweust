package me.heizi.jweust

import com.github.javaparser.utils.Utils.assertNotNull
import me.heizi.jweust.tasks.TaskCompileExe
import me.heizi.jweust.tasks.TaskJweust
import me.heizi.jweust.tasks.TaskUpdateRepo
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple unit test for the 'me.heizi.jweust.greeting' plugin.
 */
class JweustPluginTest {

    // Create a test project and apply the plugin
    private val project by lazy { ProjectBuilder.builder().build().also {
        it.plugins.run {
            apply("me.heizi.jweust")
            apply("java")
        }
        it.buildDir.resolve("libs")
            .also { it.mkdirs() }
            .resolve(it.name+".jar")
            .also { it.writeText("hash-able") }

    } }

    @Test fun `plugin registers task`() {
        assertNotNull(project.tasks.findByName(TaskJweust.NAME))
        assertNotNull(project.tasks.findByName(TaskUpdateRepo.NAME))
        assertNotNull(project.tasks.findByName(TaskCompileExe.NAME))
    }
    @Test fun `plugin registers extension`() {
        assertNotNull(project.extensions.findByName(JweustPlugin.EXTENSION_NAME))
    }
    @Test fun `plugin registers config`() {
        val projectName = "test-by-gradle"
        project.jweust {
            rustProjectName = projectName
        }
        assertEquals(projectName,project.tasks.withType(TaskJweust::class.java).first().rustProjectName)
    }

    private val taskUpdateRepo by lazy {
        project.tasks.withType(TaskUpdateRepo::class.java).first()
    }
    private val taskCompileExe by lazy {
        project.tasks.withType(TaskCompileExe::class.java).first()
    }
    @Test fun `jweust files init`() {
        with(project) {
            jweust {
                defaults()
                jweustRoot = buildDir.resolve("../../jweust")
                println(this)
            }
            extra["jweust.git.fetch"] = false
        }
        runCatching {
            taskUpdateRepo.taskAction()
        }.onFailure {
            it.printStackTrace()
        }
    }
    @Test fun `jweust files update`() {
        with(project) {
            jweust {
                defaults()
                rustProjectName = run {
                    // hash256 of timestamp
                    val time = System.currentTimeMillis().toString()
                    java.security.MessageDigest.getInstance("SHA-256").digest(time.toByteArray()).joinToString("") {
                        "%02x".format(it)
                    }
                }
                jweustRoot = buildDir.resolve("../../jweust")
                println(this)
            }
            extra["jweust.git.fetch"] = false
        }

        runCatching {
            taskUpdateRepo.taskAction()
        }.onFailure {
            it.printStackTrace()
        }
    }

    @Test fun `jweust tasks`() {
        with(project) {
            jweust {
                defaults()
                jweustRoot = buildDir.resolve("../../jweust")
                println(this)
            }
        }
    }
    @Test fun `jweust compile exe`() {
        with(project) {
            jweust {
                defaults()
                jweustRoot = buildDir.resolve("../../jweust")
                println(this)
            }
        }
        taskUpdateRepo.taskAction()
        taskCompileExe.taskAction()
    }

}

internal fun Project.jweust(function: JweustExtension.() -> Unit) {
    extensions.getByType(JweustExtension::class.java).apply(function)
}
