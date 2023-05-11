package me.heizi.jweust
import me.heizi.jweust.beans.getOrNull
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra


sealed interface JweustTasks: JweustProjectExtension {

    @Suppress("PropertyName")
    val _logger: Logger
    val rustProjectName:String
    fun getExtra(key:String):Any?

    abstract class TaskBase protected constructor(
        extension: JweustExtension,
    ) : DefaultTask(),JweustExtension by extension,JweustTasks {
        @TaskAction
        abstract fun taskAction()

        override val _logger: Logger
            @Internal
            get() = super.getLogger()

        override fun getExtra(key: String): Any? {
            return project.extra.getOrNull(key)
        }
    }
}
