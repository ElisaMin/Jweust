package org.gradle.kotlin.dsl

import me.heizi.jweust.JweustVarsExtension
import me.heizi.jweust.beans.JarConfig

/**
 * Embed jar into exe file.
 *
 * default enabled
 */
var JweustVarsExtension.embedJar: Boolean
    get() = hashOfIncludeJar?.takeIf { it.isNotBlank() } != null;
    set(embed) {
        if (!embed) hashOfIncludeJar = null
        else { includeJarByGenerate() }
    }
var JarConfig.mainClass: String?
    get() = launcher?.mainClass
    set(value) { launcher {
        mainClass = value
    } }