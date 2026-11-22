package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.python.PythonVisualTransformation
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BgSurfaceElevated
import com.example.ui.theme.BgSurfaceHover
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.FindReplaceState

private val QUICK_SYMBOLS = listOf(
    "TAB" to "\t",
    ":" to ":",
    "(" to "()",
    ")" to ")",
    "[" to "[",
    "]" to "]",
    "{" to "{",
    "}" to "}",
    "=" to "=",
    "\"" to "\"",
    "'" to "'",
    "#" to "#",
    "_" to "_",
    "." to ".",
    "," to ",",
)

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val activeTab = uiState.activeTab
    val visualTransformation = remember { PythonVisualTransformation() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PyForge",
                style = MaterialTheme.typography.titleMedium,
                color = AccentCyan,
                modifier = Modifier.padding(start = 4.dp, end = 6.dp),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = activeTab?.undoRedo?.canUndo == true,
                    modifier = Modifier.size(38.dp).testTag("editor_undo_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (activeTab?.undoRedo?.canUndo == true) TextPrimary else TextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = activeTab?.undoRedo?.canRedo == true,
                    modifier = Modifier.size(38.dp).testTag("editor_redo_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (activeTab?.undoRedo?.canRedo == true) TextPrimary else TextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }

                IconButton(
                    onClick = {
                        val txt = activeTab?.textValue?.text ?: ""
                        if (txt.isNotEmpty()) clipboard.setText(AnnotatedString(txt))
                    },
                    enabled = activeTab != null,
                    modifier = Modifier.size(38.dp).testTag("editor_copy_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy File",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = {
                        val clip = clipboard.getText()?.text
                        if (!clip.isNullOrEmpty()) viewModel.insertQuickText(clip)
                    },
                    enabled = activeTab != null,
                    modifier = Modifier.size(38.dp).testTag("editor_paste_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Paste",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFindReplace() },
                    modifier = Modifier.size(38.dp).testTag("editor_find_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Find and Replace",
                        tint = if (uiState.findReplace.visible) AccentCyan else TextSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }

                IconButton(
                    onClick = { viewModel.saveActiveTabImmediate() },
                    enabled = activeTab != null,
                    modifier = Modifier.size(38.dp).testTag("editor_save_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save File",
                        tint = if (activeTab?.isDirty == true) AccentAmber else TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = { viewModel.setConsoleVisible(!uiState.isConsoleVisible) },
                    modifier = Modifier.size(38.dp).testTag("editor_console_toggle"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "Toggle Console",
                        tint = if (uiState.isConsoleVisible) AccentCyan else TextSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (uiState.isRunning) {
                Button(
                    onClick = { viewModel.stopExecution() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCoral,
                        contentColor = BgCanvas,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp).testTag("editor_stop_button"),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = { viewModel.runCurrentFile() },
                    enabled = activeTab != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = BgCanvas,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp).testTag("editor_run_button"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Run", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // File Tabs Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurfaceElevated)
                .border(width = 0.5.dp, color = BorderSubtle)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            uiState.tabs.forEachIndexed { index, tab ->
                val selected = index == uiState.activeTabIndex
                Surface(
                    color = if (selected) BgCanvas else BgSurfaceHover.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (selected) AccentCyan.copy(alpha = 0.7f) else BorderSubtle,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { viewModel.selectTab(index) }
                        .testTag("editor_tab_$index"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    ) {
                        if (!tab.syntaxResult.valid) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentCoral)
                            )
                            Spacer(Modifier.width(6.dp))
                        } else if (tab.isDirty) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = tab.fileName,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.closeTab(index) },
                            modifier = Modifier.size(24.dp).testTag("close_tab_$index"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close ${tab.fileName}",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { viewModel.createNewScratchTab() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgSurfaceHover)
                    .testTag("editor_new_tab_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Python File",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Status Banner (e.g. inserted snippet or replaced count)
        AnimatedVisibility(visible = uiState.statusBanner != null) {
            uiState.statusBanner?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, color = AccentCyan, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.dismissBanner() }) {
                        Text("OK", color = AccentCyan)
                    }
                }
            }
        }

        // Find & Replace Bar
        AnimatedVisibility(visible = uiState.findReplace.visible) {
            FindReplaceBar(
                state = uiState.findReplace,
                onFindChange = viewModel::updateFindQuery,
                onReplaceChange = viewModel::updateReplaceQuery,
                onToggleCase = viewModel::toggleCaseSensitive,
                onPrev = { viewModel.navigateMatch(-1) },
                onNext = { viewModel.navigateMatch(1) },
                onReplace = viewModel::replaceCurrentMatch,
                onReplaceAll = viewModel::replaceAllMatches,
                onClose = { viewModel.toggleFindReplace(false) },
            )
        }

        // Inline AST Syntax Error Banner
        if (activeTab != null && !activeTab.syntaxResult.valid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentCoral.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("syntax_error_banner"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Syntax Error",
                    tint = AccentCoral,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Line ${activeTab.syntaxResult.line}: ${activeTab.syntaxResult.message}",
                    color = AccentCoral,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Code Editor Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BgCanvas),
        ) {
            if (activeTab == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No Python file open", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.createNewScratchTab() },
                        modifier = Modifier.testTag("empty_new_file_button"),
                    ) {
                        Text("Create New Script")
                    }
                }
            } else {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()
                val codeText = activeTab.textValue.text
                val lineCount = remember(codeText) {
                    var count = 1
                    for (ch in codeText) if (ch == '\n') count++
                    count
                }
                val errorLine = if (!activeTab.syntaxResult.valid) activeTab.syntaxResult.line else -1
                val lineHeightSp = (settings.fontSizeSp * 1.45f).sp

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .padding(vertical = 8.dp),
                ) {
                    if (settings.showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 42.dp)
                                .background(BgCanvas)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            for (ln in 1..lineCount) {
                                Text(
                                    text = ln.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = (settings.fontSizeSp - 1).coerceAtLeast(10).sp,
                                    lineHeight = lineHeightSp,
                                    color = if (ln == errorLine) AccentCoral else TextMuted,
                                    fontWeight = if (ln == errorLine) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }

                    BasicTextField(
                        value = activeTab.textValue,
                        onValueChange = { viewModel.onCodeChanged(it) },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = lineHeightSp,
                            color = TextPrimary,
                        ),
                        cursorBrush = SolidColor(AccentCyan),
                        visualTransformation = visualTransformation,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                            .padding(horizontal = 10.dp)
                            .widthIn(min = 600.dp)
                            .testTag("python_code_editor"),
                    )
                }
            }
        }

        // Autocomplete Suggestions Bar
        if (uiState.completions.isNotEmpty() && activeTab != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSurfaceElevated)
                    .border(width = 0.5.dp, color = BorderSubtle)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                uiState.completions.forEach { item ->
                    val badgeColor = when (item.kind) {
                        "keyword" -> AccentCoral
                        "builtin", "func" -> AccentViolet
                        "module" -> AccentEmerald
                        else -> AccentCyan
                    }
                    Surface(
                        color = BgSurfaceHover,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { viewModel.applyCompletion(item) }
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = item.kind.take(1).uppercase(),
                                color = badgeColor,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = item.label,
                                color = TextPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        // Quick Python Symbol Bar
        if (settings.showSymbolBar && activeTab != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSurface)
                    .border(width = 0.5.dp, color = BorderSubtle)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QUICK_SYMBOLS.forEach { (label, value) ->
                    Surface(
                        color = BgSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { viewModel.insertQuickText(value) }
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                    ) {
                        Text(
                            text = label,
                            color = if (label == "TAB") AccentCyan else TextPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // Fast Execution Console
        AnimatedVisibility(visible = uiState.isConsoleVisible) {
            ConsolePanel(
                entries = uiState.consoleEntries,
                isRunning = uiState.isRunning,
                isWaitingForInput = uiState.isWaitingForInput,
                inputPrompt = uiState.inputPrompt,
                lastExitCode = uiState.lastExitCode,
                lastDurationMs = uiState.lastDurationMs,
                onSubmitInput = viewModel::submitInput,
                onStop = viewModel::stopExecution,
                onClear = viewModel::clearConsole,
                onClose = { viewModel.setConsoleVisible(false) },
            )
        }
    }
}

@Composable
private fun FindReplaceBar(
    state: FindReplaceState,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onToggleCase: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurfaceElevated)
            .border(width = 0.5.dp, color = BorderSubtle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = state.findQuery,
                onValueChange = onFindChange,
                placeholder = { Text("Find...", fontSize = 12.sp, color = TextMuted) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = TextPrimary,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                ),
                modifier = Modifier.weight(1f).testTag("find_input"),
            )
            Text(
                text = if (state.matchRanges.isEmpty()) "0/0" else "${state.activeMatchIndex + 1}/${state.matchRanges.size}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextSecondary,
            )
            IconButton(onClick = onToggleCase, modifier = Modifier.size(34.dp)) {
                Text(
                    text = "Aa",
                    color = if (state.caseSensitive) AccentCyan else TextMuted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous Match", tint = TextPrimary)
            }
            IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next Match", tint = TextPrimary)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close Find", tint = TextSecondary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = state.replaceQuery,
                onValueChange = onReplaceChange,
                placeholder = { Text("Replace with...", fontSize = 12.sp, color = TextMuted) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = TextPrimary,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = BorderSubtle,
                ),
                modifier = Modifier.weight(1f).testTag("replace_input"),
            )
            TextButton(onClick = onReplace, modifier = Modifier.testTag("replace_one_button")) {
                Text("Replace", color = AccentCyan, fontSize = 12.sp)
            }
            TextButton(onClick = onReplaceAll, modifier = Modifier.testTag("replace_all_button")) {
                Text("All", color = AccentAmber, fontSize = 12.sp)
            }
        }
    }
}
