package me.heizi.jweust.beans

//pub const JRE_SEARCH_DIR:&[&str] = &["./lib/runtime"];
//pub const JRE_SEARCH_ENV:&[&str] = &["JAVA_HOME"];
//pub const JRE_OPTIONS:&[&str] = &[];
//pub const JRE_NATIVE_LIBS:&[&str] = &[];
//pub const JRE_VERSION:&[&str] = &["19.0"];
@RustParsable.Prefix("JRE_")
data class JreConfig(
    var searchDir: MutableSet<JvmSearch.JvmDir> = mutableSetOf(),
    var searchEnv : MutableSet<JvmSearch.EnvVar> = mutableSetOf(),
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

    },
    var options: MutableSet<String> = mutableSetOf(),
    @RustParsable.Name("NATIVE_LIBS")
    var nativeLibsPath: MutableSet<String> = mutableSetOf(),
    @RustParsable.Type("&[&'static str]")
    var version: Set<Number> = emptySet(),
//    val preferred: JrePreferred = JrePreferred.DefaultVM
): RustParsable {
    infix fun versions(range: ClosedFloatingPointRange<Float>) {
        version = sequenceOf(1.6f,1.7f,1.8f,9f,10f,11f,12f,13f,14f,15f,16f,17f,18f,19f,20f,21f,22f,23f,24f)
            .filter { it in range }.toSet()
    }

    override fun parsingValueExtra(name: String): (() -> String)? {
        if (name == "version") return {
            version.map { f -> when (f) {
                is Double -> f.toInt().takeIf { it>2 } ?:f
                is Float -> f.toInt().takeIf { it>2 } ?:f
                else -> f
            }.toString()
            }.joinToString(",", "&[", "]" ) {
                "\"$it\""
            }
        }
        return null
    }

}