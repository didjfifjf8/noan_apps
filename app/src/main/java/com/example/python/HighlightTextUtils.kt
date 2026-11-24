package com.example.python

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.db.HighlightSpan
import com.example.data.db.HighlighterColor

object HighlightTextUtils {

    fun buildHighlightedText(text: String, spans: List<HighlightSpan>): AnnotatedString {
        if (text.isEmpty() || spans.isEmpty()) return AnnotatedString(text)
        return buildAnnotatedString {
            append(text)
            for (span in spans) {
                val s = span.start.coerceIn(0, text.length)
                val e = span.end.coerceIn(0, text.length)
                if (e > s) {
                    val markerColor = HighlighterColor.fromKey(span.colorKey).color
                    addStyle(
                        style = SpanStyle(
                            background = markerColor.copy(alpha = 0.36f),
                        ),
                        start = s,
                        end = e,
                    )
                }
            }
        }
    }

    fun wordRangeAt(text: String, cursor: Int): IntRange? {
        if (text.isEmpty()) return null
        val idx = cursor.coerceIn(0, text.length - 1).let { pos ->
            if (pos > 0 && cursor == text.length) pos - 1
            else if (pos > 0 && text[pos].isWhitespace() && !text[pos - 1].isWhitespace()) pos - 1
            else pos
        }
        if (text[idx].isWhitespace()) return null
        var start = idx
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        var end = idx + 1
        while (end < text.length && !text[end].isWhitespace()) end++
        return if (end > start) start until end else null
    }

    fun addHighlight(
        existing: List<HighlightSpan>,
        start: Int,
        end: Int,
        colorKey: String,
    ): List<HighlightSpan> {
        val s = minOf(start, end)
        val e = maxOf(start, end)
        if (e <= s) return existing

        val out = mutableListOf<HighlightSpan>()
        for (span in existing) {
            if (span.end <= s || span.start >= e) {
                out.add(span)
            } else {
                if (span.start < s) {
                    out.add(span.copy(end = s))
                }
                if (span.end > e) {
                    out.add(span.copy(start = e))
                }
            }
        }
        out.add(HighlightSpan(s, e, colorKey))
        return out.sortedBy { it.start }
    }

    fun removeHighlightInRange(
        existing: List<HighlightSpan>,
        start: Int,
        end: Int,
    ): List<HighlightSpan> {
        val s = minOf(start, end)
        val e = maxOf(start, end)
        if (s == e) {
            // Remove any span covering cursor
            return existing.filterNot { s in it.start..it.end }
        }
        return existing.filterNot { span -> span.end > s && span.start < e }
    }

    fun adjustSpansForTextEdit(
        oldText: String,
        newText: String,
        spans: List<HighlightSpan>,
    ): List<HighlightSpan> {
        if (oldText == newText || spans.isEmpty()) return spans
        if (newText.isEmpty()) return emptyList()

        var prefix = 0
        val minLen = minOf(oldText.length, newText.length)
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }

        var oldSuffix = oldText.length
        var newSuffix = newText.length
        while (oldSuffix > prefix && newSuffix > prefix && oldText[oldSuffix - 1] == newText[newSuffix - 1]) {
            oldSuffix--
            newSuffix--
        }

        val editStart = prefix
        val deletedEnd = oldSuffix
        val delta = newText.length - oldText.length

        return spans.mapNotNull { span ->
            val ns: Int
            val ne: Int
            when {
                span.end <= editStart -> {
                    ns = span.start
                    ne = span.end
                }
                span.start >= deletedEnd -> {
                    ns = span.start + delta
                    ne = span.end + delta
                }
                else -> {
                    ns = minOf(span.start, editStart)
                    ne = (span.end + delta).coerceAtLeast(ns)
                }
            }
            val clampedS = ns.coerceIn(0, newText.length)
            val clampedE = ne.coerceIn(0, newText.length)
            if (clampedE > clampedS) span.copy(start = clampedS, end = clampedE) else null
        }
    }
}

class HighlightVisualTransformation(
    private val spans: List<HighlightSpan>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = HighlightTextUtils.buildHighlightedText(text.text, spans)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
