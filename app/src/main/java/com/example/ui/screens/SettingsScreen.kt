package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val runtimeInfo by viewModel.runtimeInfo.collectAsStateWithLifecycle()
    val siteSizeKb by viewModel.sitePackagesSizeKb.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDiagnostics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        AnimatedVisibility(visible = statusMsg != null) {
            statusMsg?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentEmerald.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, color = AccentEmerald, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.dismissStatus() }) {
                        Text("OK", color = AccentEmerald)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Editor Preferences
            SettingsSectionCard(
                title = "Editor",
                icon = Icons.Filled.Tune,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Font Size", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${settings.fontSizeSp} sp (JetBrains Mono)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setFontSize(settings.fontSizeSp - 1) },
                            modifier = Modifier.testTag("font_decrease_button"),
                        ) { Text("-", color = TextPrimary) }
                        OutlinedButton(
                            onClick = { viewModel.setFontSize(settings.fontSizeSp + 1) },
                            modifier = Modifier.testTag("font_increase_button"),
                        ) { Text("+", color = TextPrimary) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Indentation", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Spaces inserted per Tab / auto-indent", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4).forEach { spaces ->
                            FilterChip(
                                selected = settings.tabSize == spaces,
                                onClick = { viewModel.setTabSize(spaces) },
                                label = { Text("$spaces sp") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentCyan,
                                ),
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Auto-save Code",
                    subtitle = "Automatically save active Python files as you type",
                    checked = settings.autoSave,
                    onCheckedChange = viewModel::setAutoSave,
                    tag = "toggle_auto_save",
                )

                SettingsToggleRow(
                    title = "Show Line Numbers",
                    subtitle = "Display line gutter and inline syntax error markers",
                    checked = settings.showLineNumbers,
                    onCheckedChange = viewModel::setShowLineNumbers,
                    tag = "toggle_line_numbers",
                )

                SettingsToggleRow(
                    title = "Quick Symbol Bar",
                    subtitle = "Show TAB, colons, brackets and quotes above keyboard",
                    checked = settings.showSymbolBar,
                    onCheckedChange = viewModel::setShowSymbolBar,
                    tag = "toggle_symbol_bar",
                )
            }

            // Execution Engine Settings
            SettingsSectionCard(
                title = "CPython Execution",
                icon = Icons.Filled.Memory,
            ) {
                Text("Execution Timeout", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Safely interrupt runaway loops after selected duration", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15 to "15s", 30 to "30s", 60 to "60s", 120 to "2m", 0 to "Off").forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.executionTimeoutSeconds == sec,
                            onClick = { viewModel.setExecutionTimeout(sec) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AccentCyan,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Surface(
                    color = BgCanvas,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("Engine", "${runtimeInfo.implementation} ${runtimeInfo.pythonVersion}")
                        InfoRow("Mode", "100% Offline Embedded Native")
                        InfoRow("Architecture", runtimeInfo.architecture.ifEmpty { "android" })
                        InfoRow("Stdlib Modules", "${runtimeInfo.stdlibModulesCount} built-in")
                        InfoRow("3rd-Party Packages", "${runtimeInfo.installedPackagesCount} installed")
                        InfoRow("pip Storage", "${siteSizeKb} KB")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.clearPlotCache() },
                    modifier = Modifier.fillMaxWidth().testTag("clear_plot_cache_button"),
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Matplotlib Plot Cache", color = AccentCyan)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgCanvas,
                checkedTrackColor = AccentCyan,
            ),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = TextPrimary)
    }
}
