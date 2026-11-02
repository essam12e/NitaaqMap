package com.nitaaq.map.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Platform-independent QR module grid; Android renders it to a Canvas or a Bitmap. */
class QrMatrix(val size: Int, private val modules: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    companion object {
        /**
         * Encodes [content] with error-correction level M (good balance for a ~200 char URL on a
         * phone screen) and a quiet zone of [margin] modules.
         */
        fun encode(content: String, margin: Int = 2): QrMatrix {
            require(content.isNotBlank()) { "QR content must not be blank" }
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to margin,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            // Width/height 0 = one pixel per module; we scale ourselves when drawing.
            val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val size = bits.width
            val modules = BooleanArray(size * size) { i -> bits[i % size, i / size] }
            return QrMatrix(size, modules)
        }
    }
}
