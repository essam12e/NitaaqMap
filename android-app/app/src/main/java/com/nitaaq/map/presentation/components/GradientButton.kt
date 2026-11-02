package com.nitaaq.map.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.nitaaq.map.ui.theme.LocalReducedMotion
import com.nitaaq.map.ui.theme.NitaaqTheme

/**
 * The single loud element on a screen: the brand gradient pill. Loading replaces the label
 * in place (no layout jump) and blocks repeated taps.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = text,
    icon: Painter? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "press",
    )
    val active = enabled && !loading
    val colors = NitaaqTheme.colors.brandGradientColors
    val brush = if (enabled) Brush.horizontalGradient(colors) else Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 14.dp else 0.dp, CircleShape, spotColor = colors.last().copy(alpha = 0.45f), ambientColor = Color.Transparent)
            .clip(CircleShape)
            .background(brush)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = Color.White),
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { if (loading) stateDescription = loadingText }
            .heightIn(min = 58.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val content = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "loading",
        ) { isLoading ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(loadingText, style = MaterialTheme.typography.labelLarge, color = content)
                } else {
                    icon?.let {
                        Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text, style = MaterialTheme.typography.labelLarge, color = content)
                }
            }
        }
    }
}
