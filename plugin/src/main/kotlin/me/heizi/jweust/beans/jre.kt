@file:Suppress("unused")
package me.heizi.jweust.beans

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.Serializable

sealed interface JvmSearch:Serializable {
    @JvmInline
    value class JvmDir(val path: String):JvmSearch
    @JvmInline
    value class EnvVar(val name: String):JvmSearch
}


//pub const JRE_SEARCH_DIR:&[&str] = &["./lib/runtime"];
//pub const JRE_SEARCH_ENV:&[&str] = &["JAVA_HOME"];
//pub const JRE_OPTIONS:&[&str] = &[];
//pub const JRE_NATIVE_LIBS:&[&str] = &[];
//pub const JRE_VERSION:(u8,u8) = (0,255);
@RustParsable.Prefix("JRE_")
data class JreConfig(
    @get:Input
    var searchDir: MutableSet<JvmSearch.JvmDir> = mutableSetOf(),
    @get:Input
    var searchEnv: MutableSet<JvmSearch.EnvVar> = mutableSetOf(),
    @get:Input
    var options: MutableSet<String> = mutableSetOf(),
    @RustParsable.Name("NATIVE_LIBS")
    @get:Input
    var nativeLibsPath: MutableSet<String> = mutableSetOf(),
    @get:Internal
//    @RustParsable.Type("(u8,u8)")
    // min to max
    var version: Pair<UByte,UByte> = UByte.MIN_VALUE to UByte.MAX_VALUE ,
//    val preferred: JrePreferred = JrePreferred.DefaultVM
): RustParsable,Serializable {
    companion object {
        var JreConfig.min:UInt
            get() = version.first.toUInt()
            set(value) { version = value.toInt().toUByte() to version.second  }
        var JreConfig.max:UInt
            get() = version.second.toUInt()
            set(value) { version = version.first to value.toInt().toUByte() }
    }

    @get:Internal
    val search: MutableSet<JvmSearch> = object : MutableSet<JvmSearch> by mutableSetOf(), RustParsable {
        override fun add(element: JvmSearch): Boolean { return when (element) {
            is JvmSearch.JvmDir -> searchDir.add(element)
            is JvmSearch.EnvVar -> searchEnv.add(element)
        } }
        override fun addAll(elements: Collection<JvmSearch>): Boolean = elements.all { add(it) }
        override fun remove(element: JvmSearch): Boolean { return when (element) {
            is JvmSearch.JvmDir -> searchDir.remove(element)
            is JvmSearch.EnvVar -> searchEnv.remove(element)
        } }
        override fun removeAll(elements: Collection<JvmSearch>): Boolean = elements.all { remove(it) }
        override fun retainAll(elements: Collection<JvmSearch>): Boolean = elements.all { remove(it) }
        override fun clear() { searchDir.clear();searchEnv.clear() }

    }

    override fun parsingValueExtra(name: String): (() -> String)? {
        return null
    }

}