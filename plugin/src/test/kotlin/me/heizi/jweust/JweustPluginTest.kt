package me.heizi.jweust

import com.github.javaparser.utils.Utils.assertNotNull
import me.heizi.jweust.tasks.git
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
        it.plugins.apply("me.heizi.jweust")
    } }

    @Test fun `plugin registers task`() {
        assertNotNull(project.tasks.findByName(JweustTask.NAME))
    }
    @Test fun `plugin registers extension`() {
        assertNotNull(project.extensions.findByName(JweustPlugin.EXTENSION_NAME))
    }
    @Test fun `plugin registers config`() {
        val projectName = "test-by-gradle"
        project.jweust {
            rustProjectName = projectName
        }
        assertEquals(projectName,project.tasks.withType(JweustTask::class.java).first().rustProjectName)
    }

    private val task by lazy {
        project.tasks.withType(JweustTask::class.java).first()
    }
    @Test fun `jweust git`() {
        with(project) {
            jweust {
                defaults()
                jweustRoot = buildDir.resolve("../../jweust")
                println(this)
            }
            extra["jweust.git.fetch"] = false
        }
        runCatching {
            task.git()
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
        task.clone(task)
        task.parse()
        task.build(task)
    }


}

internal fun Project.jweust(function: JweustExtension.() -> Unit) {
    extensions.getByType(JweustExtension::class.java).apply(function)
}
