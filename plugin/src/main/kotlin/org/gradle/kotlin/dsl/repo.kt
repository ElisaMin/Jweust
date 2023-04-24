package org.gradle.kotlin.dsl

import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

//@Deprecated("its not usually method, it will be removed in future", level =  DeprecationLevel.HIDDEN,
//    // GitHub Packages
//    replaceWith = ReplaceWith("maven { url = URI.create(\"https://maven.pkg.github.com/ElisaMin/Khell\") }")
//)
@Suppress("FunctionName","unused")
fun RepositoryHandler.`use heizi's maven repo by github`() {
    maven {
        url = URI.create("https://raw.githubusercontent.com/ElisaMin/Maven/master/")
    }
}
