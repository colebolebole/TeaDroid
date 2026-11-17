package site.colebolebole.ezject.ui.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import site.colebolebole.ezject.data.ContentEntry
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.ui.common.EmptyState
import site.colebolebole.ezject.ui.common.ErrorState
import site.colebolebole.ezject.ui.common.LoadingBox
import site.colebolebole.ezject.ui.common.Markdown
import site.colebolebole.ezject.ui.common.Single
import site.colebolebole.ezject.ui.common.formatBytes
import site.colebolebole.ezject.ui.common.navViewModel

enum class FileKind { Text, Image, Binary, TooLarge }

data class FileContent(val entry: ContentEntry, val kind: FileKind, val text: String?)

private val imageExts = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico", "avif")
private const val MAX_TEXT_BYTES = 1_500_000L

class FileVM(h: SavedStateHandle) : ViewModel() {
    val owner: String = h.get<String>("owner").orEmpty()
    val repo: String = h.get<String>("repo").orEmpty()
    val ref: String = h.get<String>("ref").orEmpty()
    val path: String = h.get<String>("path").orEmpty()
    val name: String get() = path.substringAfterLast('/')
    private val api = Graph.api

    val file = Single(viewModelScope) {
        val entry = api.fileEntry(owner, repo, path, ref)
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        when {
            ext in imageExts -> FileContent(entry, FileKind.Image, null)
            entry.size > MAX_TEXT_BYTES -> FileContent(entry, FileKind.TooLarge, null)
            else -> {
                val bytes = if (!entry.content.isNullOrBlank() && entry.encoding == "base64") {
                    api.decodeBase64(entry.content)
                } else {
                    api.rawBytes(owner, repo, path, ref)
                }
                val sniff = bytes.take(8000)
                if (sniff.any { it == 0.toByte() }) FileContent(entry, FileKind.Binary, null)
                else FileContent(entry, FileKind.Text, String(bytes, Charsets.UTF_8))
            }
        }
    }

    init {
        file.load()
    }
}

@Composable
fun FileScreen(nav: NavController) {
    val vm = navViewModel { FileVM(it) }
    val content = vm.file.value
    var rendered by remember { mutableStateOf(true) }
    val isMarkdown = vm.name.lowercase().let { it.endsWith(".md") || it.endsWith(".markdown") }
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(vm.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${vm.repo} · ${vm.ref}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                actions = {
                    if (isMarkdown && content?.kind == FileKind.Text) {
                        IconButton(onClick = { rendered = !rendered }) {
                            Icon(if (rendered) Icons.Outlined.Code else Icons.Outlined.Description, if (rendered) "Show source" else "Show rendered")
                        }
                    }
                    if (content?.text != null) {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(content.text)) }) {
                            Icon(Icons.Outlined.ContentCopy, "Copy")
                        }
                    }
                    IconButton(onClick = { content?.entry?.htmlUrl?.let(uriHandler::openUri) }, enabled = content != null) {
                        Icon(Icons.Outlined.OpenInBrowser, "Open in browser")
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                content == null && vm.file.error != null -> ErrorState(vm.file.error ?: "", vm.file::load)
                content == null -> LoadingBox()
                else -> when (content.kind) {
                    FileKind.Image -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = Graph.api.rawUrl(vm.owner, vm.repo, vm.path, vm.ref),
                            contentDescription = vm.name,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    FileKind.Binary -> EmptyState(
                        Icons.Outlined.InsertDriveFile, "Binary file",
                        subtitle = "${formatBytes(content.entry.size)} · open it in the browser to download.",
                    )
                    FileKind.TooLarge -> EmptyState(
                        Icons.Outlined.InsertDriveFile, "File is too large to preview",
                        subtitle = "${formatBytes(content.entry.size)} · open it in the browser instead.",
                    )
                    FileKind.Text -> {
                        val text = content.text.orEmpty()
                        if (isMarkdown && rendered) {
                            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                                val dir = vm.path.substringBeforeLast('/', "")
                                Markdown(text, resolveUrl = { url ->
                                    if (url.contains("://") || url.startsWith("#") || url.startsWith("mailto:")) url
                                    else {
                                        val joined = if (url.startsWith("/")) url.trimStart('/') else listOf(dir, url).filter { it.isNotEmpty() }.joinToString("/")
                                        Graph.api.rawUrl(vm.owner, vm.repo, joined, vm.ref)
                                    }
                                })
                            }
                        } else {
                            CodeView(text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeView(text: String) {
    val lines = remember(text) { text.replace("\t", "    ").split('\n') }
    val gutter = lines.size.toString().length
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(Modifier.padding(vertical = 8.dp)) {
                itemsIndexed(lines) { i, line ->
                    Row(Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            (i + 1).toString().padStart(gutter),
                            modifier = Modifier.width((gutter * 9 + 12).dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.End,
                            softWrap = false,
                        )
                        Text(
                            line.ifEmpty { " " },
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
