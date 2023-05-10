package me.heizi.jweust.beans

import me.heizi.jweust.beans.JreConfig.Companion.min
import me.heizi.jweust.tasks.getRustFile
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginsObjectParseTest {

    @Test fun `parse default`() {
        println(config.getRustFile())
    }

    @Test fun `parse jar`() {
        val jar = config.jar
        println(jar.parsePartOfFile()+"\n"+(jar.launcher?:LauncherConfig()).parsePartOfFile())
    }
    @Test fun `parse jre`() {
        val jvm = config.jre
        println(jvm.parsePartOfFile())
    }


    companion object {
        val config = JweustConfig().apply {
            rustProjectName = "ProjectTest"
            applicationType = ApplicationType.ConsoleWhileOptionApplication()
            workdir = "."
            log {
                error = LogFileConfig(
                    path = "error.log",
                    isOverwrite = false,
                )
                stdout = null
            }
            exe {

                permissions = Permissions.HighersInTheRoom
                isInstance = true
                arch = "arm64"
                internalName = "test jweust"

                icon = "Icon.ico"
                fileDescription = "JavaWindowsExecutableRust"
                fileVersion = "0"

                productName = "test product"
                productVersion = "1.0.0"

                legalCopyright = "Jweust@Heizi"
                companyName = "Heizi"

            }

            jar {
                files = setOf("test.jar")
            }
            jre.search += JvmSearch.JvmDir("./jvm/")
            jre.search += JvmSearch.JvmDir("./lib/runtime")
            jre.search += JvmSearch.EnvVar("JAVA_HOME")
            jre.min = 19u

            charset {
                jvm = "GBK"
                stdout = "GBK"
            }

            splashScreen {
                imagePath = "splash.png"
            }
        }

    }
}

class RustParseTest {
    @Test fun `parse custom type, name, prefix `() {
        val expected = """
            pub const PRE_HELLO_WORLD:Option<(&'static str, &'static str)> = Some(("hello","world"));
        """.trimIndent()
//        println("e:| $expected")
        val actual = HelloWorld().parsePartOfFile()
//        println("a:| $actual")
        assertEquals(expected,actual)
//        assertSame(expected,actual)
    }
    @Test fun `parse nullable `() {
        val expected = """
            pub const VAR_A_N:Option<&'static str> = None;
            pub const VAR_A_S:Option<&'static str> = Some("var");
        """.trimIndent()
        val testing = object :RustParsable {
            val varAN: String? = null
            val varAS: String? = "var"
        }
        val actual = testing.parsePartOfFile()
        assertEquals(expected,actual)
    }
    @Test fun `parse array or iterable`() {
        val expected = """
            pub const ARRAYS:&[&'static str] = &["1", "2", "3"];
            pub const ITERABLES:&[&'static str] = &["1", "2", "3"];
            pub const LISTS:&[&'static str] = &["1", "2", "3"];
            pub const SETS:&[&'static str] = &["1", "2", "3"];
        """.trimIndent()
        val testing = object :RustParsable {
            val arrays: Array<String> = arrayOf("1","2","3")
            val sets: Set<String> = setOf("1","2","3")
            val lists: List<String> = listOf("1","2","3")
            val iterables: Iterable<String> = Iterable { arrays.iterator() }
        }
        val actual = testing.parsePartOfFile()
        assertEquals(expected,actual)
    }
    @Test fun `parse map`() {
        val expected = """
            pub const MAPS:&[(i32, bool)] = &[(1, true), (2, false)];
        """.trimIndent()
        val testing = object :RustParsable {
            val maps: Map<Int,Boolean> = mapOf(1 to true, 2 to false)
        }
        val actual = testing.parsePartOfFile()
        assertEquals(expected,actual)
    }
    @Test fun `parse pan test`() {
        println(PanTest().parsePartOfFile())
    }
}
@Suppress("unused")
@RustParsable.Prefix("PAN_")
private class PanTest (
    val string: String = "somestr",
    val stringNullable: String? = null,
    val stringNotNull: String? = "value here",
    val boolean: Boolean = true,
    val integer: Int = 1,
    val double: Long = 1L,
    @RustParsable.Name("custom")
    val unknown: String? = null,
    val list: List<String> = listOf("1","2","3"),
    val listNullable: List<String>? = null,
    val listNotNull: List<String>? = listOf("1","2","3"),
    val array: Array<String> = arrayOf("1","2","3"),
):RustParsable {
    companion object {
        fun result() = """
            pub const PAN_ARRAY:&[&'static str] = &["1", "2", "3"];
            pub const PAN_BOOLEAN:bool = true;
            pub const PAN_DOUBLE:i64 = 1;
            pub const PAN_INTEGER:i32 = 1;
            pub const PAN_LIST:&[&'static str] = &["1", "2", "3"];
            pub const PAN_LIST_NOT_NULL:Option<&[&'static str]> = Some(&["1", "2", "3"]);
            pub const PAN_LIST_NULLABLE:Option<&[&'static str]> = None;
            pub const PAN_STRING:&'static str = "somestr";
            pub const PAN_STRING_NOT_NULL:Option<&'static str> = Some("value here");
            pub const PAN_STRING_NULLABLE:Option<&'static str> = None;
            pub const PAN_custom:Option<&'static str> = None;
        """.trimIndent()
    }
}

@RustParsable.Prefix("PRE_")
private class HelloWorld :RustParsable {
    @RustParsable.Name("HELLO_WORLD")
    @RustParsable.Type("Option<(&'static str, &'static str)>")
    val helloW: Pair<String, String> = Pair("hello","world")
    override fun parsingValueExtra(name: String): (() -> String) {
        return {
            "Some((\"${helloW.first}\",\"${helloW.second}\"))"
        }
    }
}

