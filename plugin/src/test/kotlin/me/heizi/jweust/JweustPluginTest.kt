package me.heizi.jweust

import com.github.javaparser.utils.Utils.assertNotNull
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

    @Test fun `plugin registers config`() {
        val projectName = "test-by-gradle"
        project.Jweust {
            jweustRoot = project.buildDir.resolve("../../jweust")
            rustProjectName = projectName
        }
        assertEquals(projectName,project.tasks.withType(JweustTask::class.java).first().rustProjectName)
    }
    private val task by lazy {
        project.tasks.withType(JweustTask::class.java).first()
    }

    @Test fun `jweust tasks`() {
        task.clone().apply {
            println(this)
            task.jweustRoot.listFiles()!!
                .takeIf { it.isNotEmpty() }!!
                .forEach(::println)
        }
        task.parse()
        task.build()
    }

}
