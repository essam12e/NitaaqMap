package com.nitaaq.map.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nitaaq.map.R
import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.ui.theme.LocalReducedMotion
import com.nitaaq.map.ui.theme.NitaaqTheme

data class PhaseColors(val fg: Color, val bg: Color)

@Composable
fun phaseColors(phase: ExpiryPhase): PhaseColors {
    val c = NitaaqTheme.colors
    val target = when (phase) {
        ExpiryPhase.Active -> PhaseColors(c.success, c.successContainer)
        ExpiryPhase.Expiring -> PhaseColors(c.warning, c.warningContainer)
        ExpiryPhase.Expired -> PhaseColors(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
    }
    val fg by animateColorAsState(target.fg, label = "fg")
    val bg by animateColorAsState(target.bg, label = "bg")
    return PhaseColors(fg, bg)
}

/** Status is never conveyed by colour alone: dot + label, announced politely to TalkBack. */
@Composable
fun StatusPill(phase: ExpiryPhase, modifier: Modifier = Modifier) {
    val colors = phaseColors(phase)
    val label = when (phase) {
        ExpiryPhase.Active -> stringResource(R.string.result_status_active)
        ExpiryPhase.Expiring -> stringResource(R.string.result_status_expiring)
        ExpiryPhase.Expired -> stringResource(R.string.result_status_expired)
    }
    val reduced = LocalReducedMotion.current
    val pulse = if (phase != ExpiryPhase.Expired && !reduced) {
        val t = rememberInfiniteTransition(label = "pulse")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(if (phase == ExpiryPhase.Expiring) 500 else 1100), RepeatMode.Reverse),
            label = "dot",
        )
        a
    } else 1f
    Row(
        modifier
            .background(colors.bg, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).alpha(pulse).background(colors.fg, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.fg)
    }
}
