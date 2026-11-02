package com.nitaaq.map.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nitaaq.map.R

/** The official Nitaaq logo on a quiet tile (the logo already carries the gradient). */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.nitaaq_logo), contentDescription = stringResource(R.string.app_name))
    }
}
