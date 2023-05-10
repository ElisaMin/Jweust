package org.gradle.kotlin.dsl

import me.heizi.jweust.JweustVarsExtension

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