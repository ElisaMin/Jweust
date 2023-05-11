package me.heizi.jweust.tasks

import me.heizi.jweust.JweustExtension
import me.heizi.jweust.JweustTasks
import javax.inject.Inject

open class TaskJweust @Inject constructor (
    extension: JweustExtension
): JweustTasks.TaskBase(extension) {
    init {
        group = "jweust"
        description = "build exe from rust project after clone and parsed"
    }
    override fun taskAction() {
        _logger.lifecycle("done")
    }
    companion object {
        const val NAME = "jweust"
    }
}