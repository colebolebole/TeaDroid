package app.teadroid.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

// ---------------------------------------------------------------- model

private sealed interface Md {
    data class Heading(val level: Int, val text: String) : Md
    data class Para(val text: String) : Md
    data class Code(val code: String) : Md
    data class Quote(val text: String) : Md
    data class Item(val indent: Int, val marker: String, val task: Boolean?, val text: String) : Md
    data object Rule : Md
    data class Images(val urls: List<Pair<String, String>>) : Md // alt to url
    data class Table(val rows: List<List<String>>) : Md
}

private val headingRe = Regex("""^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$""")
private val ruleRe = Regex("""^\s{0,3}([-*_])(\s*\1){2,}\s*$""")
private val itemRe = Regex("""^(\s*)([-*+]|\d+[.)])\s+(.*)$""")
private val taskRe = Regex("""^\[([ xX])]\s+(.*)$""")
private val tableSepRe = Regex("""^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")
private val mdImageRe = Regex("""!\[([^\]]*)]\(\s*<?([^)\s>]+)>?[^)]*\)""")
private val htmlImageRe = Regex("""<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val htmlAltRe = Regex("""\balt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val htmlCommentRe = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
private val htmlTagRe = Regex("""</?[a-zA-Z][^>]*>""")
private val brRe = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

private fun imagesOf(line: String): List<Pair<String, String>> {
    val out = mutableListOf<Pair<Int, Pair<String, String>>>()
    mdImageRe.findAll(line).forEach { out += it.range.first to (it.groupValues[1] to it.groupValues[2]) }
    htmlImageRe.findAll(line).forEach {
        val alt = htmlAltRe.find(it.value)?.groupValues?.get(1).orEmpty()
        out += it.range.first to (alt to it.groupValues[1])
    }
    return out.sortedBy { it.first }.map { it.second }
}

/** True if, after removing images, link wrappers and tags, nothing meaningful is left. */
private fun isImageOnly(line: String): Boolean {
    var s = line
    s = mdImageRe.replace(s, "")
    s = htmlImageRe.replace(s, "")
    s = htmlTagRe.replace(s, "")
    s = s.replace(Regex("""\[\s*]\([^)]*\)"""), "")
    return s.isBlank()
}

private fun parseMarkdown(source: String): List<Md> {
    val lines = htmlCommentRe.replace(source.replace("\r\n", "\n"), "").split('\n')
    val out = mutableListOf<Md>()
    var i = 0
    val para = StringBuilder()
    fun flushPara() {
        if (para.isNotBlank()) out += Md.Para(para.toString().trim('\n'))
        para.clear()
    }
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") || trimmed.startsWith("~~~") -> {
                flushPara()
                val fence = trimmed.take(3)
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                    code.appendLine(lines[i]); i++
                }
                out += Md.Code(code.toString().trimEnd('\n'))
            }
            trimmed.isEmpty() -> flushPara()
            headingRe.matches(line) -> {
                flushPara()
                val m = headingRe.find(line)!!
                out += Md.Heading(m.groupValues[1].length, m.groupValues[2])
            }
            ruleRe.matches(line) -> { flushPara(); out += Md.Rule }
            trimmed.startsWith(">") -> {
                flushPara()
                val q = StringBuilder()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    q.appendLine(lines[i].trim().removePrefix(">").removePrefix(" ")); i++
                }
                i--
                out += Md.Quote(q.toString().trim('\n'))
            }
            itemRe.matches(line) -> {
                flushPara()
                val m = itemRe.find(line)!!
                val indent = m.groupValues[1].replace("\t", "    ").length / 2
                val marker = m.groupValues[2].let { if (it[0].isDigit()) it else "•" }
                var text = m.groupValues[3]
                var task: Boolean? = null
                taskRe.find(text)?.let { t -> task = t.groupValues[1] != " "; text = t.groupValues[2] }
                out += Md.Item(indent, marker, task, text)
            }
            line.contains('|') && i + 1 < lines.size && tableSepRe.matches(lines[i + 1]) -> {
                flushPara()
                fun cells(l: String) = l.trim().trim('|').split('|').map { it.trim() }
                val rows = mutableListOf(cells(line))
                i += 2
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows += cells(lines[i]); i++
                }
                i--
                out += Md.Table(rows)
            }
            imagesOf(line).isNotEmpty() && isImageOnly(line) -> {
                flushPara()
                out += Md.Images(imagesOf(line))
            }
            else -> {
                val cleaned = htmlTagRe.replace(brRe.replace(line, "\n"), "")
                if (cleaned.isBlank() && line.isNotBlank()) { /* pure HTML wrapper line */ } else {
                    para.appendLine(cleaned)
                }
            }
        }
        i++
    }
    flushPara()
    return out
}

// ---------------------------------------------------------------- inline

private class InlineStyle(val linkColor: Color, val codeBg: Color)

private fun AnnotatedString.Builder.inline(s: String, st: InlineStyle, resolve: (String) -> String) {
    var i = 0
    val n = s.length
    fun isWordChar(idx: Int) = idx in 0 until n && s[idx].isLetterOrDigit()
    while (i < n) {
        val c = s[i]
        when {
            c == '\\' && i + 1 < n && s[i + 1] in "\\`*_{}[]()#+-.!~|>" -> { append(s[i + 1]); i += 2 }
            c == '`' -> {
                var ticks = 1
                while (i + ticks < n && s[i + ticks] == '`') ticks++
                val fence = "`".repeat(ticks)
                val end = s.indexOf(fence, i + ticks)
                if (end > 0) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = st.codeBg, fontSize = 13.sp)) {
                        append(s.substring(i + ticks, end).trim())
                    }
                    i = end + ticks
                } else { append(c); i++ }
            }
            (s.startsWith("**", i) || s.startsWith("__", i)) -> {
                val mark = s.substring(i, i + 2)
                val end = s.indexOf(mark, i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { inline(s.substring(i + 2, end), st, resolve) }
                    i = end + 2
                } else { append(c); i++ }
            }
            s.startsWith("~~", i) -> {
                val end = s.indexOf("~~", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { inline(s.substring(i + 2, end), st, resolve) }
                    i = end + 2
                } else { append(c); i++ }
            }
            (c == '*' || c == '_') && !(c == '_' && isWordChar(i - 1)) -> {
                val end = s.indexOf(c, i + 1)
                if (end > i + 1 && !s[i + 1].isWhitespace() && !(c == '_' && isWordChar(end + 1))) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { inline(s.substring(i + 1, end), st, resolve) }
                    i = end + 1
                } else { append(c); i++ }
            }
            c == '!' && s.startsWith("![", i) -> {
                val m = mdImageRe.matchAt(s, i)
                if (m != null) { append(m.groupValues[1].ifBlank { "image" }); i += m.value.length } else { append(c); i++ }
            }
            c == '[' -> {
                val close = findClosingBracket(s, i)
                if (close > 0 && s.startsWith("](", close)) {
                    val urlEnd = s.indexOf(')', close + 2)
                    if (urlEnd > 0) {
                        val url = s.substring(close + 2, urlEnd).trim().substringBefore(' ').trim('<', '>')
                        val label = s.substring(i + 1, close)
                        withLink(LinkAnnotation.Url(resolve(url), TextLinkStyles(SpanStyle(color = st.linkColor, textDecoration = TextDecoration.Underline)))) {
                            inline(label, st, resolve)
                        }
                        i = urlEnd + 1
                    } else { append(c); i++ }
                } else { append(c); i++ }
            }
            s.startsWith("http://", i) || s.startsWith("https://", i) -> {
                var end = i
                while (end < n && !s[end].isWhitespace() && s[end] != '<' && s[end] != ')') end++
                val raw = s.substring(i, end)
                val url = raw.trimEnd('.', ',', ';', ':', '!', '?')
                withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = st.linkColor, textDecoration = TextDecoration.Underline)))) {
                    append(url)
                }
                i += url.length
            }
            else -> { append(c); i++ }
        }
    }
}

private fun findClosingBracket(s: String, open: Int): Int {
    var depth = 0
    for (j in open until s.length) {
        when (s[j]) {
            '[' -> depth++
            ']' -> { depth--; if (depth == 0) return j }
        }
    }
    return -1
}

// ---------------------------------------------------------------- rendering

/**
 * Lightweight Markdown renderer (headings, lists, tasks, quotes, code, tables, links, images).
 * [resolveUrl] maps relative URLs (README images / links) to absolute ones.
 */
@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    resolveUrl: (String) -> String = { it },
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val style = InlineStyle(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is Md.Heading -> {
                    val ts = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        buildAnnotatedString { inline(block.text, style, resolveUrl) },
                        style = ts,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp),
                    )
                }
                is Md.Para -> Text(
                    buildAnnotatedString { inline(block.text, style, resolveUrl) },
                    style = MaterialTheme.typography.bodyMedium,
                )
                is Md.Code -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        block.code,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp),
                        softWrap = false,
                    )
                }
                is Md.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Surface(
                        modifier = Modifier.width(4.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(2.dp),
                    ) {}
                    Text(
                        buildAnnotatedString { inline(block.text, style, resolveUrl) },
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is Md.Item -> Row(Modifier.padding(start = (block.indent * 16).dp)) {
                    val marker = when (block.task) {
                        true -> "☑"
                        false -> "☐"
                        null -> block.marker
                    }
                    Text(marker, modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        buildAnnotatedString { inline(block.text, style, resolveUrl) },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Md.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                is Md.Images -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    block.urls.forEach { (alt, url) ->
                        AsyncImage(
                            model = resolveUrl(url),
                            contentDescription = alt,
                            modifier = Modifier.heightIn(max = 240.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                is Md.Table -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.horizontalScroll(rememberScrollState())) {
                        block.rows.forEachIndexed { r, row ->
                            Row(
                                Modifier
                                    .background(if (r == 0) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        buildAnnotatedString { inline(cell, style, resolveUrl) },
                                        modifier = Modifier.width(140.dp).padding(horizontal = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
