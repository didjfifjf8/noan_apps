package com.example.python

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class UndoRedoManager(
    initialValue: TextFieldValue = TextFieldValue(""),
    private val maxHistory: Int = 100,
) {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var current: TextFieldValue = initialValue

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun reset(value: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        current = value
    }

    fun recordChange(next: TextFieldValue) {
        if (next.text == current.text) {
            current = next
            return
        }
        undoStack.addLast(current)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        current = next
    }

    fun undo(): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        current = undoStack.removeLast()
        return current
    }

    fun redo(): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        current = redoStack.removeLast()
        return current
    }
}

object PythonAutoIndent {
    /**
     * Applies Python smart indentation when the user inserts a newline.
     * Returns null if no auto-indent adjustment is needed.
     */
    fun applyOnNewline(previous: TextFieldValue, next: TextFieldValue, tabSize: Int): TextFieldValue? {
        if (next.text.length != previous.text.length + 1) return null
        val cursor = next.selection.start
        if (cursor <= 0 || cursor > next.text.length) return null
        if (next.text[cursor - 1] != '\n') return null

        val prevCursor = cursor - 1
        val lineStart = next.text.lastIndexOf('\n', prevCursor - 1).let { if (it == -1) 0 else it + 1 }
        val prevLine = next.text.substring(lineStart, prevCursor)

        var indentCount = 0
        while (indentCount < prevLine.length && prevLine[indentCount] == ' ') {
            indentCount++
        }
        val trimmed = prevLine.trimEnd()
        val extraIndent = if (trimmed.endsWith(":")) tabSize else 0
        val indentStr = " ".repeat(indentCount + extraIndent)
        if (indentStr.isEmpty()) return null

        val updatedText = buildString(next.text.length + indentStr.length) {
            append(next.text, 0, cursor)
            append(indentStr)
            append(next.text, cursor, next.text.length)
        }
        val newCursor = cursor + indentStr.length
        return TextFieldValue(
            text = updatedText,
            selection = TextRange(newCursor),
        )
    }
}
