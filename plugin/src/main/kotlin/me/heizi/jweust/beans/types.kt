package me.heizi.jweust.beans

sealed interface ApplicationType {
    object Console: ApplicationType
    object Application: ApplicationType
    class ConsoleWhileOptionApplication(
        val whileCommand: String = "-DConsole=true"
    ): ApplicationType
}
@Suppress("NOTHING_TO_INLINE")
internal inline fun ApplicationType.getRustValue() = when (this) {
    is ApplicationType.Console -> "None"
    is ApplicationType.Application -> "Some(None)"
    is ApplicationType.ConsoleWhileOptionApplication -> "Some(Some(\"${this.whileCommand}\"))"
}
