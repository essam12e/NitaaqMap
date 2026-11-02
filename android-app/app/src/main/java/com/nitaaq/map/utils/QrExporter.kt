package com.nitaaq.map.utils

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.nitaaq.map.R
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.qr.QrMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Share via the Android Sharesheet (FileProvider) and save via MediaStore — no permissions needed. */
class QrExporter(private val context: Context) {
    private val renderer = QrImageRenderer(context)

    suspend fun saveToGallery(trip: TemporaryTrip): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = render(trip)
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "nitaaq-trip-${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NitaaqMap")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert failed")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed" }
                } ?: throw IOException("No output stream")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }
    }

    /** Returns a ready-to-launch chooser intent with the QR image and the temporary link. */
    suspend fun shareIntent(trip: TemporaryTrip): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            // Only one shared image is kept; older ones are overwritten.
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "nitaaq-trip.png")
            file.outputStream().use { render(trip).compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val text = context.getString(R.string.result_share_text, formatClockTime(trip.expiresAtMs), trip.tripUrl)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, context.getString(R.string.result_share_chooser))
        }
    }

    private fun render(trip: TemporaryTrip): Bitmap =
        renderer.render(QrMatrix.encode(trip.tripUrl, margin = 0), formatClockTime(trip.expiresAtMs))
}
