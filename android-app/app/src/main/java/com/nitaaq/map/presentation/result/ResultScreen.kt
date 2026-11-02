package com.nitaaq.map.presentation.result

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitaaq.map.R
import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.core.domain.formatRemaining
import com.nitaaq.map.presentation.components.CountdownBar
import com.nitaaq.map.presentation.components.GradientButton
import com.nitaaq.map.presentation.components.QrCodeView
import com.nitaaq.map.presentation.components.StatusPill
import com.nitaaq.map.presentation.components.phaseColors
import com.nitaaq.map.presentation.home.ErrorCard
import com.nitaaq.map.ui.theme.CountdownStyle
import com.nitaaq.map.ui.theme.LocalReducedMotion
import com.nitaaq.map.ui.theme.NitaaqTheme
import com.nitaaq.map.ui.theme.Spacing
import com.nitaaq.map.utils.formatClockTime
import com.nitaaq.map.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onBack: () -> Unit,
    onNewLink: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val saved = stringResource(R.string.result_saved)
    val saveFailed = stringResource(R.string.result_save_failed)
    val shareFailed = stringResource(R.string.result_share_failed)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultEvent.LaunchShare -> {
                    val launched = runCatching { context.startActivity(event.intent) }.isSuccess
                    if (!launched) snackbar.showSnackbar(shareFailed)
                }
                ResultEvent.Saved -> { haptics.confirm(); snackbar.showSnackbar(saved) }
                ResultEvent.SaveFailed -> { haptics.reject(); snackbar.showSnackbar(saveFailed) }
                ResultEvent.ShareFailed -> { haptics.reject(); snackbar.showSnackbar(shareFailed) }
                ResultEvent.Expired -> haptics.reject()
                ResultEvent.Renewed -> haptics.confirm()
                ResultEvent.RenewFailed -> haptics.reject()
            }
        }
    }

    // Keep the screen awake while a valid code is on display, so the driver can scan it.
    val phase = state.countdown?.phase
    val view = LocalView.current
    DisposableEffect(phase) {
        view.keepScreenOn = phase != null && phase != ExpiryPhase.Expired
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    phase?.let { StatusPill(it, Modifier.padding(end = Spacing.md)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val trip = state.trip
        val qr = state.qr
        val countdown = state.countdown
        if (state.loaded && (trip == null || qr == null || countdown == null)) {
            EmptyState(onNewLink, Modifier.padding(padding))
            return@Scaffold
        }
        if (trip == null || qr == null || countdown == null) return@Scaffold

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val wide = maxWidth >= 600.dp
            val qrCard = @Composable { m: Modifier ->
                QrCard(state = state, modifier = m)
            }
            val details = @Composable { m: Modifier ->
                Details(
                    state = state,
                    onShare = { haptics.tap(); viewModel.share() },
                    onSave = { haptics.tap(); viewModel.save() },
                    onNew = { viewModel.discard(); onNewLink() },
                    onRenew = { haptics.tap(); viewModel.renew() },
                    modifier = m,
                )
            }
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    qrCard(Modifier.weight(1f).widthIn(max = 420.dp))
                    details(Modifier.weight(1f).verticalScroll(rememberScrollState()))
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    qrCard(Modifier.widthIn(max = 420.dp).fillMaxWidth())
                    Spacer(Modifier.height(Spacing.lg))
                    details(Modifier.widthIn(max = 560.dp).fillMaxWidth())
                    Spacer(Modifier.height(Spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun QrCard(state: ResultUiState, modifier: Modifier = Modifier) {
    val countdown = state.countdown ?: return
    val qr = state.qr ?: return
    val expired = countdown.phase == ExpiryPhase.Expired
    val ring = phaseColors(countdown.phase)
    val reduced = LocalReducedMotion.current
    val dim by animateFloatAsState(if (expired) 0.12f else 1f, tween(if (reduced) 0 else 450), label = "dim")
    val border by animateColorAsState(ring.fg.copy(alpha = if (expired) 0.6f else 0.35f), label = "ring")

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = NitaaqTheme.colors.qrPaper,
        border = BorderStroke(2.dp, border),
        shadowElevation = if (NitaaqTheme.colors.isDark) 0.dp else 6.dp,
    ) {
        Box(Modifier.padding(Spacing.lg), contentAlignment = Alignment.Center) {
            // Re-key on the token so a renewed code animates in.
            AnimatedContent(
                targetState = state.trip?.id to qr,
                contentKey = { it.first },
                transitionSpec = {
                    if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(150))
                },
                label = "qr",
            ) { target ->
                QrCodeView(
                    matrix = target.second,
                    ink = NitaaqTheme.colors.qrInk,
                    paper = NitaaqTheme.colors.qrPaper,
                    description = stringResource(R.string.result_qr_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(dim)
                        .then(if (expired) Modifier.blur(6.dp) else Modifier),
                )
            }
            AnimatedVisibility(visible = expired, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(64.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.result_status_expired),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun Details(
    state: ResultUiState,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip = state.trip ?: return
    val countdown = state.countdown ?: return
    val colors = MaterialTheme.colorScheme
    val phaseColor = phaseColors(countdown.phase)
    val expired = countdown.phase == ExpiryPhase.Expired
    val remainingLabel = stringResource(R.string.result_remaining)
    val time = formatRemaining(countdown.remainingMs)

    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(remainingLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(
                    time,
                    style = CountdownStyle,
                    color = if (countdown.phase == ExpiryPhase.Active) colors.onBackground else phaseColor.fg,
                    modifier = Modifier.semantics { contentDescription = "$remainingLabel $time" },
                )
            }
            Text(
                stringResource(R.string.result_valid_until, formatClockTime(trip.expiresAtMs)),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        CountdownBar(
            fraction = countdown.fraction,
            track = colors.surfaceContainerHigh,
            fill = if (countdown.phase == ExpiryPhase.Active) NitaaqTheme.colors.brandGradient else SolidColor(phaseColor.fg),
        )
        Spacer(Modifier.height(Spacing.md))
        AnimatedContent(targetState = countdown.phase, label = "hint", transitionSpec = { fadeIn() togetherWith fadeOut() }) { p ->
            Text(
                stringResource(
                    when (p) {
                        ExpiryPhase.Active -> R.string.result_hint_active
                        ExpiryPhase.Expiring -> R.string.result_hint_expiring
                        ExpiryPhase.Expired -> R.string.result_hint_expired
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        AnimatedContent(targetState = expired, label = "actions", transitionSpec = { fadeIn() togetherWith fadeOut() }) { isExpired ->
            if (!isExpired) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilledTonalButton(
                            onClick = onShare,
                            enabled = !state.busyExporting,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text(stringResource(R.string.result_share), style = MaterialTheme.typography.labelLarge)
                        }
                        FilledTonalButton(
                            onClick = onSave,
                            enabled = !state.busyExporting,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_download), null, Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text(stringResource(R.string.result_save), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = onNew,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        border = BorderStroke(1.dp, colors.outline),
                    ) {
                        Icon(painterResource(R.drawable.ic_qr), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.result_new), style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    state.renewError?.let {
                        ErrorCard(it, onRetry = onRenew)
                        Spacer(Modifier.height(Spacing.sm))
                    }
                    if (trip.sourceUrl.isNotBlank()) {
                        GradientButton(
                            text = stringResource(R.string.result_renew),
                            loadingText = stringResource(R.string.home_creating),
                            onClick = onRenew,
                            loading = state.renewing,
                            icon = rememberVectorPainterRefresh(),
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    TextButton(onClick = onNew, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.result_another_link), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberVectorPainterRefresh() = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Refresh)

@Composable
private fun EmptyState(onNewLink: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_qr), null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.result_no_trip), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.lg))
        GradientButton(text = stringResource(R.string.home_create_qr), onClick = onNewLink, modifier = Modifier.widthIn(max = 360.dp))
    }
}

