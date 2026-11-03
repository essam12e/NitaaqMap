package com.nitaaq.map.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.nitaaq.map.R
import com.nitaaq.map.core.qr.QrMatrix

/**
 * Renders the shareable/saveable image: a crisp QR (pixel-aligned modules, always dark on white
 * for scanners) plus a small branded caption with the server expiry time.
 */
class QrImageRenderer(private val context: Context) {

    fun render(matrix: QrMatrix, validUntil: String): Bitmap {
        val width = 1080
        val module = (width - 2 * PADDING) / matrix.size
        val qrSide = module * matrix.size
        val qrLeft = (width - qrSide) / 2f
        val qrTop = PADDING.toFloat()
        val height = PADDING + qrSide + CAPTION_HEIGHT

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val ink = Paint().apply { color = NAVY; isAntiAlias = false; style = Paint.Style.FILL }
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    val l = qrLeft + x * module
                    val t = qrTop + y * module
                    canvas.drawRect(RectF(l, t, l + module, t + module), ink)
                }
            }
        }

        val bold = ResourcesCompat.getFont(context, R.font.tajawal_extrabold) ?: Typeface.DEFAULT_BOLD
        val regular = ResourcesCompat.getFont(context, R.font.tajawal_medium) ?: Typeface.DEFAULT
        val center = width / 2f
        val base = qrTop + qrSide + 96f

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 56f; color = NAVY; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.app_name), center, base, title)

        val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 38f; color = MUTED; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.qr_image_caption), center, base + 66f, caption)
        canvas.drawText(context.getString(R.string.result_valid_until, validUntil), center, base + 122f, caption)

        // Brand rule: the website's blue → green, used once.
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(center - 90f, 0f, center + 90f, 0f, BLUE, GREEN, android.graphics.Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(center - 90f, base + 158f, center + 90f, base + 168f), 5f, 5f, rule)
        return bitmap
    }

    private companion object {
        const val PADDING = 96
        const val CAPTION_HEIGHT = 330
        val NAVY = Color.rgb(0x0B, 0x1A, 0x2B)
        val MUTED = Color.rgb(0x4A, 0x5F, 0x75)
        val BLUE = Color.rgb(0x25, 0x63, 0xEB)
        val GREEN = Color.rgb(0x22, 0xC5, 0x5E)
    }
}
