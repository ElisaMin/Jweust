package me.heizi.jweust

import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.system.exitProcess


internal interface RustParsable {
    fun parsingValueExtra(name: String):(()->String)? {
        return null
    }
    @Target(AnnotationTarget.PROPERTY,AnnotationTarget.CLASS)
    annotation class Name(val name:String)
    @Target(AnnotationTarget.PROPERTY)
    annotation class Type(val type:String)
    annotation class Prefix(val prefix:String)
}

internal fun RustParsable.parse():String {
    return this::class.memberProperties.joinToString("\n") { prop ->
        prop.isAccessible = true
        prop.run {
            val name =  annotations.filterIsInstance<RustParsable.Name>().firstOrNull()?.name
                ?: name.toSnackCase().uppercase().let {
                    val prefix =
                        this@parse::class.annotations.filterIsInstance<RustParsable.Name>().firstOrNull()?.name ?: ""
                    prefix + it
                }
            val prefix = this@parse::class.annotations.filterIsInstance<RustParsable.Prefix>().firstOrNull()?.prefix ?: ""
            val (type:String, value:String) = annotations.filterIsInstance<RustParsable.Type>().firstOrNull()?.type?.let {
                it to parsingValueExtra(name)!!()
            } ?: run {
                returnType.parseRust() to getter.call(this@parse).parseRust(wrap = returnType.isMarkedNullable)
            }
            isAccessible = false
            "pub const $prefix$name:$type = $value;"
        }
    }
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
        else -> "None"
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
        else -> "None"
    }
}

internal fun String.toSnackCase():String {
    return this.replace(Regex("([A-Z])")) {
        "_${it.value.lowercase()}"
    }
}