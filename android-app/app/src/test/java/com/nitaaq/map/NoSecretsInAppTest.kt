package com.nitaaq.map

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the rule "no server secrets in the APK": scans every source/resource/config file of the
 * Android project for the server's secret name, its dev fallback value, and private key material.
 * (The APK itself is additionally checked in CI with `unzip -p | grep`.)
 */
class NoSecretsInAppTest {
    private val forbidden = listOf(
        "dev-only-change-this-secret-before-production-deploy",
        "BEGIN PRIVATE KEY",
        "createHmac",
    )

    @Test
    fun androidProjectContainsNoServerSecrets() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val hits = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".gradle", "test", "tools") }
            .filter { it.isFile && it.extension in setOf("kt", "kts", "xml", "properties", "toml", "pro", "json") }
            .flatMap { f -> forbidden.filter { f.readText().contains(it) }.map { "${f.relativeTo(root)}: $it" } }
            .toList()
        assertTrue("Secrets found: $hits", hits.isEmpty())
        val secretVar = root.walkTopDown()
            .onEnter { it.name !in setOf("build", ".gradle", "test", "tools") }
            .filter { it.isFile && it.extension in setOf("kt", "kts", "xml", "properties", "toml") }
            .filter { it.readText().contains("TRIP_TOKEN_SECRET") }
            .toList()
        // The name may only appear in comments that explain it stays on the server.
        secretVar.forEach { f ->
            f.readLines().filter { "TRIP_TOKEN_SECRET" in it }.forEach { line ->
                assertTrue("${f.name}: $line", line.trimStart().startsWith("//") || line.trimStart().startsWith("*") || line.trimStart().startsWith("#"))
            }
        }
    }
}
