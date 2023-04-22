package me.heizi.jweust

import java.io.Serializable
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
internal fun JweustConfig.getRustFile():String {
    return arrayOf(
        this,
        log,
        exe,
        jar, jar.launcher ?: LauncherConfig(),
        jre,
        charset,
        (splashScreen ?: SplashScreenConfig(null)),
    )
        .asSequence()
        .map { it.parse() }
        .flatMap { it.lines() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}


internal interface RustParsable:Serializable {
    fun parsingValueExtra(name: String):(()->String)? {
        return null
    }
    @Target(AnnotationTarget.PROPERTY)
    annotation class Name(val name:String)
    @Target(AnnotationTarget.PROPERTY)
    annotation class Type(val type:String)
    @Target(AnnotationTarget.CLASS)
    annotation class Prefix(val prefix:String)
}
private val KAnnotatedElement.parseName get() =
    annotations.filterIsInstance<RustParsable.Name>().firstOrNull()?.name
private val KAnnotatedElement.parsePrefix get() =
    annotations.filterIsInstance<RustParsable.Prefix>().firstOrNull()?.prefix
private val KAnnotatedElement.parseType get() =
    annotations.filterIsInstance<RustParsable.Type>().firstOrNull()?.type
private inline val RustParsable.self get() = this
context(RustParsable)
private fun KProperty1<out RustParsable,*>.parseRust(prefix: String?):String? = getter.call(self).let { value ->
    if (value is RustParsable) return@let null else buildString {
        append("pub const ")
        //name
        prefix?.let { append(it) }
        append(parseName ?: name.toSnackCase().uppercase())
        //type
        append(':')
        val extraType = parseType
        append(extraType?: returnType.parseRust())
        //sign
        append(" = ")
        //value
        append(
            if (extraType == null) value.parseRust(wrap = returnType.isMarkedNullable)
            else parsingValueExtra(name)!!()
        )
        //end
        append(';')
    }
}

internal fun RustParsable.parse():String {
    val prefix = this::class.parsePrefix
    return this::class.memberProperties.mapNotNull {
        it.isAccessible = true
        it.parseRust(prefix)
    }.joinToString("\n")
}

fun KClass<*>.toRustType(
    arguments: List<KTypeProjection> = emptyList(),
):String = when {
    java.isArray || isSubclassOf(Iterable::class) || isSubclassOf(Map::class) -> {
        arguments.map {
            it.type?.parseRust() ?: "None"
        }.let {
            if (it.size > 1 ) it.joinToString(", ", "(", ")")
            else it.firstOrNull() ?: "None"
        }.let {type ->
            "&[$type]"
        }
    }
    else -> when (this) {
        String::class -> "&'static str"
        Int::class -> "i32"
        Long::class -> "i64"
        Double::class -> "f64"
        Boolean::class -> "bool"
        else -> when {
            isSubclassOf(JvmSearch::class) -> String::class.toRustType()
            else -> "None"
        }
    }
}
fun KType.parseRust(nullable:Boolean = isMarkedNullable):String  =
    when {
        nullable -> buildString {
            append("Option<")
            append(this@parseRust.parseRust(false))
            append(">")
        }
        classifier!=null -> (classifier as KClass<*>).toRustType(arguments)
        else -> "None"
    }

fun Any?.parseRust(wrap: Boolean = false):String {
    if (wrap && this != null ) return "Some(${this.parseRust()})"
    return when (this) {
        is String -> this.replace("\"","\\\"").let {
            "\"$it\""
        }
        is Boolean -> this.toString()
        is Number -> this.toString()
        is Array<*> -> asIterable().parseRust()
        is Pair<*,*> -> "(${first.parseRust()}, ${second.parseRust()})"
        is Map<*,*> -> map {(k,v) ->
            k to v
        }.parseRust()
        is Iterable<*> -> {
            joinToString { it.parseRust() }.let {
                "&[$it]"
            }
        }
        is JvmSearch -> when(this) {
            is JvmSearch.EnvVar -> name.parseRust()
            is JvmSearch.JvmDir -> path.parseRust()
        }
        else -> "None"
    }
}

internal fun String.toSnackCase():String {
    return this.replace(Regex("([A-Z])")) {
        "_${it.value.lowercase()}"
    }
}