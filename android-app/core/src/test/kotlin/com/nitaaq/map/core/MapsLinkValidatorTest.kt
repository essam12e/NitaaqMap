package com.nitaaq.map.core

import com.nitaaq.map.core.domain.LinkCheck
import com.nitaaq.map.core.domain.MapsLinkValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour tests for the app-side validator, written as intent ("the driver goes to the
 * destination") rather than as parity data. The parity test covers byte-for-byte agreement
 * with the server; these make sure the agreed behaviour is the right one.
 */
class MapsLinkValidatorTest {

    private fun pin(lat: String, lng: String) = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
    private fun route(encoded: String) = "https://www.google.com/maps/dir/?api=1&destination=$encoded"

    private fun assertValid(input: String, expected: String) {
        assertTrue("should accept: $input", MapsLinkValidator.isGoogleMapsUrl(input))
        assertEquals("normalize: $input", expected, MapsLinkValidator.normalize(input))
        assertEquals(LinkCheck.Valid(expected), MapsLinkValidator.check(input))
    }

    private fun assertInvalid(input: String) {
        assertFalse("should reject: $input", MapsLinkValidator.isGoogleMapsUrl(input))
    }

    // --- Directions: final destination, never the origin -------------------------------------

    @Test
    fun directionsWithCoordinatesGoesToDestination() {
        assertValid("https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925", pin("21.4858", "39.1925"))
    }

    @Test
    fun multiStopRouteGoesToLastStop() {
        assertValid(
            "https://www.google.com/maps/dir/24.7136,46.6753/26.4207,50.0888/21.4858,39.1925",
            pin("21.4858", "39.1925"),
        )
    }

    @Test
    fun mapViewAndDataAfterWaypointsAreIgnored() {
        assertValid(
            "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925/@23.1,43.0,7z/data=!3m1!4b1!4m2!4m1!3e0",
            pin("21.4858", "39.1925"),
        )
    }

    @Test
    fun namedDestinationBecomesDirectionsToThatName() {
        assertValid("https://www.google.com/maps/dir/Riyadh/Jeddah+Corniche/@22,40,6z/data=!4m2", route("Jeddah%20Corniche"))
    }

    @Test
    fun arabicDestinationIsDecodedAndReEncoded() {
        assertValid(
            "https://www.google.com/maps/dir/%D8%A7%D9%84%D8%B1%D9%8A%D8%A7%D8%B6/%D8%AC%D8%AF%D8%A9",
            route("%D8%AC%D8%AF%D8%A9"),
        )
    }

    @Test
    fun emptyOriginAndTrailingSlash() {
        assertValid("https://www.google.com/maps/dir//24.7,46.6/", pin("24.7", "46.6"))
    }

    @Test
    fun apiDestinationParameter() {
        assertValid(
            "https://www.google.com/maps/dir/?api=1&origin=24.7,46.6&destination=21.4858,39.1925",
            pin("21.4858", "39.1925"),
        )
        assertValid(
            "https://www.google.com/maps/dir/?api=1&origin=Riyadh&destination=Jeddah%20Corniche&travelmode=driving",
            route("Jeddah%20Corniche"),
        )
    }

    @Test
    fun malformedEscapeIsKeptLiterally() {
        assertValid("https://www.google.com/maps/dir/A/%E0%A4%A", route("%25E0%25A4%25A"))
    }

    @Test
    fun directionsInsideSharedText() {
        assertValid(
            "الطريق: https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925 .",
            pin("21.4858", "39.1925"),
        )
    }

    // --- Places and map views ----------------------------------------------------------------

    @Test
    fun placeUsesDataPinNotViewCentre() {
        assertValid(
            "https://www.google.com/maps/place/Kingdom+Centre/@24.71,46.67,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d24.7113!4d46.6744",
            pin("24.7113", "46.6744"),
        )
    }

    @Test
    fun placeWithoutPinIsKept() {
        val url = "https://www.google.com/maps/place/Riyadh/@24.7136,46.6753,12z"
        assertValid(url, url)
    }

    @Test
    fun mapViewAndQueryCoordinates() {
        assertValid("google.com/maps/@21.5,39.2,14z", pin("21.5", "39.2"))
        assertValid("https://google.com/maps?q=24.7,46.6", pin("24.7", "46.6"))
        assertValid("https://www.google.com/maps/search/24.7136,+46.6753", pin("24.7136", "46.6753"))
    }

    @Test
    fun normalizationIsIdempotent() {
        listOf(
            "https://www.google.com/maps/dir/24.7,46.6/21.4858,39.1925",
            "https://www.google.com/maps/dir/Riyadh/Jeddah+Corniche",
            "https://www.google.com/maps/dir/%D8%A7%D9%84%D8%B1%D9%8A%D8%A7%D8%B6/%D8%AC%D8%AF%D8%A9",
            "24.7136,46.6753",
        ).forEach {
            val once = MapsLinkValidator.normalize(it)
            assertEquals(once, MapsLinkValidator.normalize(once))
            assertTrue(MapsLinkValidator.isGoogleMapsUrl(once))
        }
    }

    // --- Coordinates -------------------------------------------------------------------------

    @Test
    fun validCoordinatesIncludingLimits() {
        assertValid("90,180", pin("90", "180"))
        assertValid("-90,-180", pin("-90", "-180"))
        assertValid("24.7136 , 46.6753", pin("24.7136", "46.6753"))
        assertValid("24.7136, 46.6753", pin("24.7136", "46.6753"))
        assertValid("موقعي 24.7136, 46.6753 الآن", pin("24.7136", "46.6753"))
        assertValid("خطأ 95,10 الصحيح 21.4858,39.1925", pin("21.4858", "39.1925"))
    }

    @Test
    fun outOfRangeCoordinatesAreRejectedNotTruncated() {
        listOf("91,46", "-91,46", "90.0001,46", "24.7136,181", "24.7136,-180.5", "191,46", "24.7,1812", "1000,10", "موقعي 91,46 الآن")
            .forEach {
                assertInvalid(it)
                assertFalse(it, MapsLinkValidator.isCoordinates(it))
                assertEquals(LinkCheck.Invalid, MapsLinkValidator.check(it))
            }
    }

    @Test
    fun lookalikeHostsAreRejected() {
        listOf(
            "https://evil.com/?q=google.com/maps",
            "https://evil.com/?next=https://maps.app.goo.gl/x",
            "https://notgoogle.com/maps",
            "https://mapsgoogle.com/maps",
            "ftp://maps.google.com/maps",
        ).forEach { assertInvalid(it) }
    }
}
