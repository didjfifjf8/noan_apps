package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.python.LibraryCategory
import com.example.python.LibraryItem
import com.example.python.PyPiSearchResult
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibrariesViewModel
import com.example.ui.viewmodel.LibraryFilter

@Composable
fun LibrariesScreen(
    viewModel: LibrariesViewModel,
    onInsertImportToEditor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val displayedItems = remember(uiState.catalog, uiState.selectedFilter, uiState.searchQuery) {
        val base = when (uiState.selectedFilter) {
            LibraryFilter.INSTALLED -> uiState.catalog.installed
            LibraryFilter.STDLIB -> uiState.catalog.stdlib
            LibraryFilter.PYPI_PURE -> uiState.catalog.available
            LibraryFilter.INCOMPATIBLE -> uiState.catalog.incompatible
        }
        val q = uiState.searchQuery.trim().lowercase()
        if (q.isEmpty()) base else base.filter {
            it.name.lowercase().contains(q) || it.summary.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Libraries", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = "CPython ${uiState.runtimeInfo.pythonVersion} • ${uiState.catalog.installed.size} 3rd-party • ${uiState.catalog.stdlib.size} stdlib",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            IconButton(
                onClick = { viewModel.refreshCatalog() },
                modifier = Modifier.testTag("libraries_refresh_button"),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Libraries", tint = AccentCyan)
            }
        }

        // Search & PyPI Lookup Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Search installed or PyPI package...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (uiState.searchQuery.isNotBlank()) {
                            viewModel.searchOnPyPi(uiState.searchQuery)
                        }
                    }
                ),
                modifier = Modifier.weight(1f).testTag("libraries_search_input"),
            )
            Button(
                onClick = { viewModel.searchOnPyPi(uiState.searchQuery) },
                enabled = uiState.searchQuery.isNotBlank() && !uiState.isSearchingPyPi,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgCanvas),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(50.dp).testTag("libraries_pypi_lookup_button"),
            ) {
                if (uiState.isSearchingPyPi) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BgCanvas, strokeWidth = 2.dp)
                } else {
                    Text("PyPI", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilter.entries.forEach { filter ->
                val count = when (filter) {
                    LibraryFilter.INSTALLED -> uiState.catalog.installed.size
                    LibraryFilter.STDLIB -> uiState.catalog.stdlib.size
                    LibraryFilter.PYPI_PURE -> uiState.catalog.available.size
                    LibraryFilter.INCOMPATIBLE -> uiState.catalog.incompatible.size
                }
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { viewModel.selectFilter(filter) },
                    label = { Text("${filter.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AccentCyan,
                    ),
                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                )
            }
        }

        // Status Toast / Message Banner
        AnimatedVisibility(visible = uiState.statusMessage != null) {
            uiState.statusMessage?.let { msg ->
                Surface(
                    color = BgSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(msg, color = TextPrimary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.dismissStatus() }) {
                            Text("OK", color = AccentCyan)
                        }
                    }
                }
            }
        }

        // Live pip Installation Progress Card
        AnimatedVisibility(visible = uiState.isInstalling || uiState.installLogs.isNotEmpty()) {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.isInstalling) "Installing ${uiState.installingPackageName}..." else "pip Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentCyan,
                            modifier = Modifier.weight(1f),
                        )
                        if (!uiState.isInstalling) {
                            TextButton(onClick = { viewModel.clearInstallLogs() }) {
                                Text("Clear", color = TextSecondary)
                            }
                        }
                    }
                    if (uiState.isInstalling) {
                        LinearProgressIndicator(
                            progress = { (uiState.installProgressPercent / 100f).coerceIn(0f, 1f) },
                            color = AccentEmerald,
                            trackColor = BgSurfaceElevated,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = uiState.installLogs.takeLast(6).joinToString("\n"),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        // PyPI Search Result Card
        uiState.pypiSearchResult?.let { pypi ->
            PyPiResultCard(
                result = pypi,
                isInstalling = uiState.isInstalling,
                onInstall = { viewModel.installPackage(pypi.name, upgrade = pypi.installed) },
                onUninstall = { viewModel.uninstallPackage(pypi.name) },
            )
        }

        // Libraries List
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else if (displayedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Extension, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No matching libraries in this tab", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                    if (uiState.searchQuery.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap 'PyPI' to search and install '${uiState.searchQuery.trim()}'",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().testTag("libraries_list"),
            ) {
                items(displayedItems, key = { "${it.category}_${it.name}" }) { item ->
                    LibraryItemCard(
                        item = item,
                        isInstalling = uiState.isInstalling,
                        onVerifyImport = { viewModel.verifyImport(item) },
                        onOpenDocs = { viewModel.openDocumentation(item) },
                        onInsertImport = { onInsertImportToEditor(item.importName) },
                        onInstallOrUpdate = {
                            viewModel.installPackage(item.name, upgrade = item.installed)
                        },
                        onUninstall = { viewModel.uninstallPackage(item.name) },
                    )
                }
            }
        }
    }

    // Documentation Viewer Dialog
    uiState.activeDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { viewModel.closeDocumentation() },
            containerColor = BgSurface,
            title = {
                Text("Docs: ${doc.name}", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .background(BgCanvas, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = if (doc.ok) doc.doc else "Unable to load documentation: ${doc.error}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = if (doc.ok) TextPrimary else AccentCoral,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeDocumentation() }) {
                    Text("Close", color = AccentCyan)
                }
            },
        )
    }
}

@Composable
private fun PyPiResultCard(
    result: PyPiSearchResult,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    Surface(
        color = BgSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!result.ok) {
                Text("PyPI Lookup Failed", style = MaterialTheme.typography.titleMedium, color = AccentCoral)
                Text(result.error, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    BadgePill(text = "v${result.version}", color = AccentCyan)
                }
                Text(result.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (!result.compatible) {
                    Surface(
                        color = AccentCoral.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(result.incompatibilityReason, style = MaterialTheme.typography.bodySmall, color = AccentCoral)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!result.installed) {
                            Button(
                                onClick = onInstall,
                                enabled = !isInstalling,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = BgCanvas),
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("pip install")
                            }
                        } else if (result.removable) {
                            val hasUpdate = result.installedVersion != null && result.installedVersion != result.version
                            if (hasUpdate) {
                                Button(
                                    onClick = onInstall,
                                    enabled = !isInstalling,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgCanvas),
                                ) {
                                    Text("Update to ${result.version}")
                                }
                            }
                            OutlinedButton(onClick = onUninstall, enabled = !isInstalling) {
                                Text("Uninstall", color = AccentCoral)
                            }
                        } else {
                            BadgePill(text = "Bundled Native (${result.installedVersion})", color = AccentEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    isInstalling: Boolean,
    onVerifyImport: () -> Unit,
    onOpenDocs: () -> Unit,
    onInsertImport: () -> Unit,
    onInstallOrUpdate: () -> Unit,
    onUninstall: () -> Unit,
) {
    val (badgeLabel, badgeColor) = when (item.category) {
        LibraryCategory.STDLIB -> "Stdlib" to AccentViolet
        LibraryCategory.BUNDLED_NATIVE -> "Bundled Native" to AccentEmerald
        LibraryCategory.PIP_RUNTIME -> "pip Installed" to AccentCyan
        LibraryCategory.PIP_AVAILABLE -> "Pure Python" to AccentAmber
        LibraryCategory.INCOMPATIBLE -> "Android Incompatible" to AccentCoral
    }

    Surface(
        color = BgSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = item.name,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        )
                        Text(
                            text = item.version,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted,
                        )
                    }
                }
                BadgePill(text = badgeLabel, color = badgeColor)
            }

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!item.compatible && item.incompatibilityReason.isNotEmpty()) {
                Text(
                    text = item.incompatibilityReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCoral,
                )
            }

            if (item.requires.isNotEmpty()) {
                Text(
                    text = "Deps: ${item.requires.take(4).joinToString(", ") { it.substringBefore(';') }}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Action Strip
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.installed) {
                    SmallActionChip(
                        label = "Import",
                        icon = Icons.Filled.Code,
                        tint = AccentCyan,
                        onClick = onInsertImport,
                    )
                    SmallActionChip(
                        label = "Docs",
                        icon = Icons.Filled.Description,
                        tint = TextPrimary,
                        onClick = onOpenDocs,
                    )
                    SmallActionChip(
                        label = "Verify",
                        icon = Icons.Filled.CheckCircle,
                        tint = AccentEmerald,
                        onClick = onVerifyImport,
                    )
                }

                if (item.category == LibraryCategory.PIP_AVAILABLE) {
                    SmallActionChip(
                        label = "pip install",
                        icon = Icons.Filled.CloudDownload,
                        tint = AccentEmerald,
                        enabled = !isInstalling,
                        onClick = onInstallOrUpdate,
                    )
                }

                if (item.category == LibraryCategory.PIP_RUNTIME && item.removable) {
                    SmallActionChip(
                        label = "Update",
                        icon = Icons.Filled.SystemUpdateAlt,
                        tint = AccentAmber,
                        enabled = !isInstalling,
                        onClick = onInstallOrUpdate,
                    )
                    SmallActionChip(
                        label = "Uninstall",
                        icon = Icons.Filled.DeleteOutline,
                        tint = AccentCoral,
                        enabled = !isInstalling,
                        onClick = onUninstall,
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SmallActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        color = BgSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else TextMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = if (enabled) tint else TextMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}
