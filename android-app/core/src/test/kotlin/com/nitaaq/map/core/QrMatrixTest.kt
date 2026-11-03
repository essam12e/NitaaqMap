package com.nitaaq.map.core

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.nitaaq.map.core.qr.QrMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QrMatrixTest {

    /** Renders the matrix to pixels and decodes it back with an independent reader. */
    private fun decode(matrix: QrMatrix, scale: Int = 6): String {
        val px = matrix.size * scale
        val pixels = IntArray(px * px) { i ->
            val x = (i % px) / scale
            val y = (i / px) / scale
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(px, px, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun roundTripsARealisticSignedTripUrl() {
        // Real token shape: base64url(JSON payload with the maps link) + "." + base64url(HMAC-SHA256)
        val tripUrl = "https://nitaaq-map.vercel.app/trip/" +
            "eyJ2IjoxLCJ1cmwiOiJodHRwczovL3d3dy5nb29nbGUuY29tL21hcHMvc2VhcmNoLz9hcGk9MSZxdWVyeT0yNC43MTM2LDQ2LjY3NTMiLCJpYXQiOjE4MDAwMDAwMDAwMDAsImV4cCI6MTgwMDAwMDMwMDAwMCwibm9uY2UiOiJhYmNkZWZnaGlqa2wifQ" +
            ".Zm9vYmFyYmF6cXV4Zm9vYmFyYmF6cXV4Zm9vYmFyYmF6cXU"
        val matrix = QrMatrix.encode(tripUrl)
        assertEquals(tripUrl, decode(matrix))
    }

    @Test
    fun qrContainsTheTemporaryLinkNotTheMapsLink() {
        val tripUrl = "https://nitaaq-map.vercel.app/trip/abc.def"
        assertEquals(tripUrl, decode(QrMatrix.encode(tripUrl)))
    }

    @Test
    fun blankContentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { QrMatrix.encode(" ") }
    }
}
