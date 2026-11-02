package com.nitaaq.map.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nitaaq.map.core.qr.QrMatrix
import kotlin.math.floor

/** Draws the QR directly on a Canvas: pixel-snapped modules, so it stays sharp at any size. */
@Composable
fun QrCodeView(
    matrix: QrMatrix,
    ink: Color,
    paper: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description },
    ) {
        drawRect(paper)
        val module = floor(size.minDimension / matrix.size)
        val offset = (size.minDimension - module * matrix.size) / 2f
        for (y in 0 until matrix.size) {
            var x = 0
            while (x < matrix.size) {
                if (matrix[x, y]) {
                    // Merge horizontal runs into one rect: fewer draw calls, no hairline seams.
                    var run = 1
                    while (x + run < matrix.size && matrix[x + run, y]) run++
                    drawRect(ink, Offset(offset + x * module, offset + y * module), Size(module * run, module))
                    x += run
                } else {
                    x++
                }
            }
        }
    }
}
