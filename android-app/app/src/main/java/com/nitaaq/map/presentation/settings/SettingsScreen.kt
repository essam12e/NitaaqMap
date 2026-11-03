package com.nitaaq.map.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitaaq.map.BuildConfig
import com.nitaaq.map.R
import com.nitaaq.map.presentation.components.BrandMark
import com.nitaaq.map.ui.theme.Spacing
import com.nitaaq.map.ui.theme.ThemeMode
import com.nitaaq.map.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, apiHost: String, onBack: () -> Unit) {
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val open = { url: String -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Section(stringResource(R.string.settings_appearance)) {
                    Column(Modifier.selectableGroup()) {
                        listOf(
                            ThemeMode.System to R.string.theme_system,
                            ThemeMode.Light to R.string.theme_light,
                            ThemeMode.Dark to R.string.theme_dark,
                        ).forEachIndexed { i, (m, label) ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .selectable(selected = mode == m, role = Role.RadioButton) {
                                        haptics.tap(); viewModel.setThemeMode(m)
                                    }
                                    .padding(horizontal = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                RadioButton(selected = mode == m, onClick = null)
                            }
                        }
                    }
                }

                Section(stringResource(R.string.settings_about)) {
                    Column(Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandMark(size = 44.dp)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            stringResource(R.string.settings_about_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow(stringResource(R.string.settings_server), apiHost)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val privacy = BuildConfig.PRIVACY_POLICY_URL
                    InfoRow(
                        stringResource(R.string.settings_privacy),
                        if (privacy.isBlank()) stringResource(R.string.settings_privacy_unavailable) else "↗",
                        onClick = privacy.takeIf { it.isNotBlank() }?.let { { open(it); Unit } },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow(stringResource(R.string.settings_website), "↗", onClick = { open(BuildConfig.WEBSITE_URL); Unit })
                }
                Text(
                    stringResource(R.string.settings_licenses),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs, top = Spacing.sm),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { Column { content() } }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.selectable(selected = false, role = Role.Button, onClick = onClick) else base)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
