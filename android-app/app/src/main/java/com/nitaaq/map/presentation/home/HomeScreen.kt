package com.nitaaq.map.presentation.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitaaq.map.R
import com.nitaaq.map.core.domain.formatRemaining
import com.nitaaq.map.presentation.Countdown
import com.nitaaq.map.presentation.UiError
import com.nitaaq.map.presentation.components.BrandMark
import com.nitaaq.map.presentation.components.GradientButton
import com.nitaaq.map.ui.theme.NitaaqTheme
import com.nitaaq.map.ui.theme.Spacing
import com.nitaaq.map.utils.rememberHaptics
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenResult: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active by viewModel.activeTrip.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val clipboardEmpty = stringResource(R.string.home_clipboard_empty)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.OpenResult -> { haptics.confirm(); onOpenResult() }
                HomeEvent.Failed -> haptics.reject()
            }
        }
    }

    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        viewModel.createQr()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                GradientButton(
                    text = stringResource(R.string.home_create_qr),
                    loadingText = stringResource(R.string.home_creating),
                    onClick = { haptics.tap(); submit() },
                    enabled = state.input.isNotBlank(),
                    loading = state.isLoading,
                    icon = painterResource(R.drawable.ic_qr),
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                TopRow(onOpenSettings)
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedVisibility(
                    visible = active != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    active?.let { ActiveTripBanner(it, onOpenResult, Modifier.padding(top = Spacing.lg)) }
                }

                Spacer(Modifier.height(Spacing.lg))
                LinkInputCard(
                    state = state,
                    onValueChange = viewModel::onInputChange,
                    onClear = viewModel::clearInput,
                    onPaste = {
                        val text = readClipboard(context)
                        if (text.isNullOrBlank()) {
                            haptics.reject()
                            scope.launch { snackbar.showSnackbar(clipboardEmpty) }
                        } else {
                            haptics.tap()
                            viewModel.onInputChange(text.trim())
                        }
                    },
                    onDone = submit,
                )

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    state.error?.let { ErrorCard(it, onRetry = submit, Modifier.padding(top = Spacing.md)) }
                }

                Spacer(Modifier.height(Spacing.xl))
                HowItWorks()
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun TopRow(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 48.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkInputCard(
    state: HomeUiState,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val success = NitaaqTheme.colors.success
    val borderColor by animateColorAsState(
        when (state.fieldState) {
            LinkFieldState.Valid -> success
            LinkFieldState.Invalid -> colors.error
            LinkFieldState.Idle -> colors.outline
        },
        label = "border",
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = if (NitaaqTheme.colors.isDark) 0.dp else 2.dp,
    ) {
        Column(Modifier.padding(Spacing.md + Spacing.xxs)) {
            Text(stringResource(R.string.home_input_label), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = state.input,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                placeholder = { Text(stringResource(R.string.home_input_placeholder), style = MaterialTheme.typography.bodyMedium) },
                // The link itself is Latin/LTR; let it render naturally inside the RTL layout.
                textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.medium,
                isError = state.fieldState == LinkFieldState.Invalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                trailingIcon = if (state.input.isNotEmpty() && !state.isLoading) {
                    { IconButton(onClick = onClear) { Icon(Icons.Filled.Clear, stringResource(R.string.home_clear)) } }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                    errorBorderColor = colors.error,
                    focusedContainerColor = colors.surfaceContainerLow,
                    unfocusedContainerColor = colors.surfaceContainerLow,
                ),
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onPaste, enabled = !state.isLoading) {
                    Icon(painterResource(R.drawable.ic_paste), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.home_paste), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(Spacing.sm))
                ValidationHint(state.fieldState, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ValidationHint(fieldState: LinkFieldState, modifier: Modifier = Modifier) {
    Box(modifier) {
        AnimatedVisibility(visible = fieldState != LinkFieldState.Idle, enter = fadeIn(), exit = fadeOut()) {
            val valid = fieldState == LinkFieldState.Valid
            val tint = if (valid) NitaaqTheme.colors.success else MaterialTheme.colorScheme.error
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (valid) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(if (valid) R.string.home_valid_link else R.string.error_invalid),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
            }
        }
    }
}

@Composable
private fun ActiveTripBanner(countdown: Countdown, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = NitaaqTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = c.successContainer,
        onClick = onOpen,
    ) {
        Row(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_timer), null, tint = c.success, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "${stringResource(R.string.home_active_trip)} · ${formatRemaining(countdown.remainingMs)}",
                style = MaterialTheme.typography.titleSmall,
                color = c.success,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpen) { Text(stringResource(R.string.home_active_trip_action), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
fun ErrorCard(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val message = error.serverMessage ?: if (error.formatArg != null) {
        stringResource(error.messageRes, error.formatArg)
    } else {
        stringResource(error.messageRes)
    }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = colors.errorContainer) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (error.isConnectivity) painterResource(R.drawable.ic_cloud_off) else painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer, modifier = Modifier.weight(1f))
            if (error.retryable) {
                Spacer(Modifier.width(Spacing.xs))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun HowItWorks() {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(stringResource(R.string.home_how_title), style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf(R.string.home_step_1, R.string.home_step_2, R.string.home_step_3).forEachIndexed { i, res ->
                Step(number = i + 1, label = stringResource(res), Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_shield), null, tint = NitaaqTheme.colors.success, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.home_how_body), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Step(number: Int, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .background(colors.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(28.dp).background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
    }
}

private fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

