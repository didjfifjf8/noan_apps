package com.example.python

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.SyntaxBuiltin
import com.example.ui.theme.SyntaxClass
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxDecorator
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.TextPrimary

object PythonSyntaxHighlighter {

    private val keywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await",
        "break", "class", "continue", "def", "del", "elif", "else", "except",
        "finally", "for", "from", "global", "if", "import", "in", "is",
        "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
        "try", "while", "with", "yield", "match", "case"
    )

    private val builtins = setOf(
        "abs", "all", "any", "ascii", "bin", "bool", "breakpoint", "bytearray",
        "bytes", "callable", "chr", "classmethod", "compile", "complex", "delattr",
        "dict", "dir", "divmod", "enumerate", "eval", "exec", "filter", "float",
        "format", "frozenset", "getattr", "globals", "hasattr", "hash", "help",
        "hex", "id", "input", "int", "isinstance", "issubclass", "iter", "len",
        "list", "locals", "map", "max", "memoryview", "min", "next", "object",
        "oct", "open", "ord", "pow", "print", "property", "range", "repr",
        "reversed", "round", "set", "setattr", "slice", "sorted", "staticmethod",
        "str", "sum", "super", "tuple", "type", "vars", "zip", "self", "cls"
    )

    private val keywordStyle = SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Medium)
    private val builtinStyle = SpanStyle(color = SyntaxBuiltin)
    private val stringStyle = SpanStyle(color = SyntaxString)
    private val commentStyle = SpanStyle(color = SyntaxComment)
    private val numberStyle = SpanStyle(color = SyntaxNumber)
    private val functionStyle = SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Medium)
    private val classStyle = SpanStyle(color = SyntaxClass, fontWeight = FontWeight.Medium)
    private val decoratorStyle = SpanStyle(color = SyntaxDecorator)
    private val operatorStyle = SpanStyle(color = SyntaxOperator)

    fun highlight(code: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val limit = minOf(code.length, 45_000)

        return buildAnnotatedString {
            append(code)
            var i = 0
            var expectDefName = false
            var expectClassName = false

            while (i < limit) {
                val c = code[i]

                // Comment
                if (c == '#') {
                    val start = i
                    while (i < limit && code[i] != '\n') i++
                    addStyle(commentStyle, start, i)
                    expectDefName = false
                    expectClassName = false
                    continue
                }

                // String prefix f/r/b/u before quote
                if ((c == 'f' || c == 'F' || c == 'r' || c == 'R' || c == 'b' || c == 'B' || c == 'u' || c == 'U') &&
                    i + 1 < limit && (code[i + 1] == '"' || code[i + 1] == '\'')
                ) {
                    val start = i
                    i++
                    i = scanString(code, i, limit)
                    addStyle(stringStyle, start, i)
                    expectDefName = false
                    expectClassName = false
                    continue
                }

                // String literal
                if (c == '"' || c == '\'') {
                    val start = i
                    i = scanString(code, i, limit)
                    addStyle(stringStyle, start, i)
                    expectDefName = false
                    expectClassName = false
                    continue
                }

                // Decorator
                if (c == '@') {
                    val start = i
                    i++
                    while (i < limit && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '.')) {
                        i++
                    }
                    addStyle(decoratorStyle, start, i)
                    continue
                }

                // Number literal
                if (c.isDigit()) {
                    val start = i
                    while (i < limit && (code[i].isLetterOrDigit() || code[i] == '.' || code[i] == '_')) {
                        i++
                    }
                    addStyle(numberStyle, start, i)
                    expectDefName = false
                    expectClassName = false
                    continue
                }

                // Identifier / keyword
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < limit && (code[i].isLetterOrDigit() || code[i] == '_')) {
                        i++
                    }
                    val word = code.substring(start, i)
                    when {
                        expectDefName -> {
                            addStyle(functionStyle, start, i)
                            expectDefName = false
                        }
                        expectClassName -> {
                            addStyle(classStyle, start, i)
                            expectClassName = false
                        }
                        word == "def" -> {
                            addStyle(keywordStyle, start, i)
                            expectDefName = true
                        }
                        word == "class" -> {
                            addStyle(keywordStyle, start, i)
                            expectClassName = true
                        }
                        word in keywords -> {
                            addStyle(keywordStyle, start, i)
                        }
                        word in builtins -> {
                            addStyle(builtinStyle, start, i)
                        }
                        i < limit && code[i] == '(' -> {
                            addStyle(functionStyle, start, i)
                        }
                    }
                    continue
                }

                // Operators
                if (c in "+-*/%=<>!&|^~:") {
                    addStyle(operatorStyle, i, i + 1)
                    i++
                    continue
                }

                if (!c.isWhitespace()) {
                    expectDefName = false
                    expectClassName = false
                }
                i++
            }

            if (limit < code.length) {
                addStyle(SpanStyle(color = TextPrimary), limit, code.length)
            }
        }
    }

    private fun scanString(code: String, startQuote: Int, limit: Int): Int {
        val quote = code[startQuote]
        // Triple quote check
        if (startQuote + 2 < limit && code[startQuote + 1] == quote && code[startQuote + 2] == quote) {
            var i = startQuote + 3
            while (i < limit) {
                if (code[i] == '\\') {
                    i += 2
                    continue
                }
                if (i + 2 < limit && code[i] == quote && code[i + 1] == quote && code[i + 2] == quote) {
                    return i + 3
                }
                i++
            }
            return limit
        }

        var i = startQuote + 1
        while (i < limit) {
            val ch = code[i]
            if (ch == '\\') {
                i += 2
                continue
            }
            if (ch == quote || ch == '\n') {
                return if (ch == quote) i + 1 else i
            }
            i++
        }
        return limit
    }
}

class PythonVisualTransformation : VisualTransformation {
    private var lastRaw: String = ""
    private var lastHighlighted: AnnotatedString = AnnotatedString("")

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw != lastRaw) {
            lastRaw = raw
            lastHighlighted = PythonSyntaxHighlighter.highlight(raw)
        }
        return TransformedText(lastHighlighted, OffsetMapping.Identity)
    }
}
