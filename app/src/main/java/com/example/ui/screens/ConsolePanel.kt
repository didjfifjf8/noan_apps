package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
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
import com.example.ui.viewmodel.ConsoleEntry
import com.example.ui.viewmodel.ConsoleEntryType

@Composable
fun ConsolePanel(
    entries: List<ConsoleEntry>,
    isRunning: Boolean,
    isWaitingForInput: Boolean,
    inputPrompt: String,
    lastExitCode: Int?,
    lastDurationMs: Long?,
    onSubmitInput: (String) -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    val panelHeight = if (expanded) 380.dp else 230.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface)
            .border(width = 1.dp, color = BorderSubtle)
            .testTag("console_panel"),
    ) {
        // Console Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = "Console",
                tint = if (isRunning) AccentEmerald else AccentCyan,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Console",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Status Pill
            val (statusText, statusColor) = when {
                isWaitingForInput -> "Waiting input()" to AccentAmber
                isRunning -> "Running..." to AccentEmerald
                lastExitCode == 0 -> "Exit 0 • ${lastDurationMs ?: 0}ms" to AccentEmerald
                lastExitCode != null -> "Exit $lastExitCode • ${lastDurationMs ?: 0}ms" to AccentCoral
                else -> "Ready" to TextMuted
            }
            Surface(
                color = statusColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isRunning) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp).testTag("console_stop_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop Execution",
                        tint = AccentCoral,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            IconButton(
                onClick = {
                    val allText = entries.filter { it.type != ConsoleEntryType.PLOT }.joinToString("") { it.text }
                    clipboard.setText(AnnotatedString(allText))
                },
                modifier = Modifier.size(36.dp).testTag("console_copy_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Console Output",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp).testTag("console_clear_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear Console",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(36.dp).testTag("console_expand_button"),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = if (expanded) "Collapse Console" else "Expand Console",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp).testTag("console_close_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Hide Console",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Streaming Output Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .background(BgCanvas)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "Tap Run (▶) to execute your Python script locally with CPython 3.11.",
                    color = TextMuted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            when (entry.type) {
                                ConsoleEntryType.PLOT -> {
                                    val bitmap = remember(entry.text) {
                                        BitmapFactory.decodeFile(entry.text)?.asImageBitmap()
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Matplotlib Output Plot",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .padding(vertical = 6.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                                        )
                                    }
                                }
                                ConsoleEntryType.STDERR -> {
                                    Text(
                                        text = entry.text.trimEnd('\n'),
                                        color = AccentCoral,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                    )
                                }
                                ConsoleEntryType.STATUS -> {
                                    Text(
                                        text = entry.text,
                                        color = AccentCyan,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                                else -> {
                                    Text(
                                        text = entry.text.trimEnd('\n'),
                                        color = TextPrimary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Stdin Input Bar (active while script is running)
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isWaitingForInput) AccentCyan.copy(alpha = 0.08f) else BgSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isWaitingForInput) {
                                inputPrompt.ifEmpty { "Enter input() and press Send..." }
                            } else {
                                "stdin (available for input())"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = if (isWaitingForInput) AccentAmber else TextMuted,
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TextPrimary,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isWaitingForInput) AccentAmber else AccentCyan,
                        unfocusedBorderColor = if (isWaitingForInput) AccentAmber.copy(alpha = 0.7f) else BorderSubtle,
                        cursorColor = AccentCyan,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            onSubmitInput(inputText)
                            inputText = ""
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("console_stdin_input"),
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        onSubmitInput(inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isWaitingForInput) AccentAmber else AccentCyan.copy(alpha = 0.2f))
                        .testTag("console_stdin_send"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send stdin",
                        tint = if (isWaitingForInput) BgCanvas else AccentCyan,
                    )
                }
            }
        }
    }
}
