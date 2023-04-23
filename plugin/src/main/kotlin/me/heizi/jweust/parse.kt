package me.heizi.jweust

import java.io.Serializable
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


internal fun String.createValidatedVersionOf(len:Int):String = buildString {
    val r = this@createValidatedVersionOf.split(".").map { it.toIntOrNull()?.toString()?:"0" }
    repeat(len) {
        if (it!=0) append('.')
        append(r.getOrNull(it) ?: "0")
    }
}
internal fun JweustConfig.getRustFile():String {
    workdir = workdir?.takeIf { it.isNotBlank() }
    exe {
        productVersion = productVersion.createValidatedVersionOf(4)
        fileVersion = fileVersion.createValidatedVersionOf(4)
    }

    return arrayOf(
        this,
        log,
        exe,
        jar, jar.launcher ?: LauncherConfig(),
        jre,
        charset,
        splashScreen?:SplashScreenConfig(),
    )
        .asSequence()
        .map { it.parsePartOfFile() }
        .flatMap { it.lines() }
        .filter { it.isNotEmpty() }
        .joinToString("\n", prefix = "#![allow(dead_code)]\n")
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
private fun KProperty1<out RustParsable,*>.parseLine(prefix: String?):String? =
    // test if is RustParseable its Nested, so we can parse it
    if (returnType.classifier.let {
        it is KClass<*> && it.isSubclassOf(RustParsable::class)
    }) null
    else buildString {
        //start
        append("pub const ")
        //name
        prefix?.let { append(it) }
        append(parseName ?: name.toSnackCase().uppercase())
        //type
        append(':')
        val extraType = parseType
        append(extraType?: returnType.getRustType())
        //sign
        append(" = ")
        //value
        append(
            if (extraType != null) parsingValueExtra(name)!!()
            else getter.call(self).getRustValue(wrap = returnType.isMarkedNullable)
        )
        //end
        append(';')
    }


internal fun RustParsable.parsePartOfFile():String {
    val prefix = this::class.parsePrefix
    return this::class.memberProperties.mapNotNull {
        it.isAccessible = true
        it.parseLine(prefix)
    }.joinToString("\n")
}

internal fun KClass<*>.toRustType(
    arguments: List<KTypeProjection> = emptyList(),
):String = when {
    java.isArray || isSubclassOf(Iterable::class) || isSubclassOf(Map::class) -> {
        arguments.map {
            it.type?.getRustType() ?: "None"
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
internal fun KType.getRustType(nullable:Boolean = isMarkedNullable):String  =
    when {
        (classifier as KClass<*>) == RustParsable::class -> getRustType(nullable = false)
        nullable -> buildString {
            append("Option<")
            append(this@getRustType.getRustType(false))
            append(">")
        }
        classifier!=null -> (classifier as KClass<*>).toRustType(arguments)
        else -> "None"
    }

internal fun Any?.getRustValue(wrap: Boolean = false):String {
    if (wrap && this != null ) return "Some(${this.getRustValue()})"
    return when (this) {
        is String -> this.replace("\"","\\\"").let {
            "\"$it\""
        }
        is Boolean -> this.toString()
        is Number -> this.toString()
        is Array<*> -> asIterable().getRustValue()
        is Pair<*,*> -> "(${first.getRustValue()}, ${second.getRustValue()})"
        is Map<*,*> -> map {(k,v) ->
            k to v
        }.getRustValue()
        is Iterable<*> -> {
            joinToString { it.getRustValue() }.let {
                "&[$it]"
            }
        }
        is JvmSearch -> when(this) {
            is JvmSearch.EnvVar -> name.getRustValue()
            is JvmSearch.JvmDir -> path.getRustValue()
        }
        else -> "None"
    }
}

internal fun String.toSnackCase():String {
    return this.replace(Regex("([A-Z])")) {
        "_${it.value.lowercase()}"
    }
}