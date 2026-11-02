package com.nitaaq.map

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.qr.QrMatrix
import com.nitaaq.map.utils.QrExporter
import com.nitaaq.map.utils.QrImageRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QrExportTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val tripUrl = "https://nitaaq-map.vercel.app/trip/eyJ2IjoxfQ.c2lnbmF0dXJl"

    private fun decode(bitmap: Bitmap): String {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, px)))).text
    }

    @Test
    fun savedAndSharedImageDecodesToTheTemporaryLink() {
        val bitmap = QrImageRenderer(context).render(QrMatrix.encode(tripUrl, margin = 0), "10:42 م")
        assertEquals(tripUrl, decode(bitmap))
    }

    @Test
    fun shareIntentCarriesImageAndLinkWithReadGrant() = runBlocking {
        val trip = TemporaryTrip(
            "eyJ2IjoxfQ.c2lnbmF0dXJl", tripUrl, "https://maps.app.goo.gl/x", 1_800_000_300_000L,
            ExpiryTimeline.fromServer(1_800_000_300_000L, 1_800_000_000_000L, 1_800_000_000_000L, 0, 1),
        )
        val chooser = QrExporter(context).shareIntent(trip).getOrThrow()
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/png", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains(tripUrl))
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertTrue(uri.authority!!.endsWith(".fileprovider"))
    }
}
