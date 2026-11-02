package com.nitaaq.map.core

import com.nitaaq.map.core.domain.LinkCheck
import com.nitaaq.map.core.domain.MapsLinkValidator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every case in maps_url_cases.json was produced by running the web project's own
 * lib/maps-url.ts (tools/generate-validator-fixtures.mjs). The Kotlin port must agree on
 * both the accept/reject decision and the normalized link, otherwise the app would show
 * "valid" for links the server rejects (or the opposite).
 */
class MapsLinkValidatorParityTest {

    @Serializable
    data class Case(val input: String, val valid: Boolean, val normalized: String)

    private val cases: List<Case> by lazy {
        val text = javaClass.classLoader!!.getResource("maps_url_cases.json")!!.readText()
        Json.decodeFromString(text)
    }

    @Test
    fun fixtureIsNotEmpty() {
        assertTrue(cases.size >= 40)
    }

    @Test
    fun acceptRejectMatchesServer() {
        val mismatches = cases.filter { MapsLinkValidator.isGoogleMapsUrl(it.input) != it.valid }
        assertEquals("mismatched decisions: ${mismatches.map { it.input }}", emptyList<Case>(), mismatches)
    }

    @Test
    fun normalizationMatchesServer() {
        val mismatches = cases.filter { MapsLinkValidator.normalize(it.input) != it.normalized }
            .map { "${it.input} -> kotlin=${MapsLinkValidator.normalize(it.input)} ts=${it.normalized}" }
        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun checkClassifiesForUi() {
        assertEquals(LinkCheck.Empty, MapsLinkValidator.check("  "))
        assertEquals(LinkCheck.Invalid, MapsLinkValidator.check("https://example.com/maps"))
        assertEquals(
            LinkCheck.Valid("https://maps.app.goo.gl/AbCdEf123"),
            MapsLinkValidator.check("شوف موقعي: https://maps.app.goo.gl/AbCdEf123 وصلني"),
        )
    }

    @Test
    fun rejectsNonGoogleHostsThatLookSimilar() {
        listOf(
            "https://google.com.evil.com/maps",
            "https://maps.apple.com/?q=Riyadh",
            "javascript:alert(1)",
            "http://goo.gl/abc",
        ).forEach { assertTrue(it, !MapsLinkValidator.isGoogleMapsUrl(it)) }
    }
}
