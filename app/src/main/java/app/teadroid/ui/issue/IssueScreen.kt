package app.teadroid.ui.issue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.teadroid.data.Comment
import app.teadroid.data.Graph
import app.teadroid.data.IssueBundle
import app.teadroid.ui.common.Avatar
import app.teadroid.ui.common.ErrorState
import app.teadroid.ui.common.LabelChip
import app.teadroid.ui.common.LoadingBox
import app.teadroid.ui.common.Markdown
import app.teadroid.ui.common.MetaText
import app.teadroid.ui.common.RefreshBox
import app.teadroid.ui.common.SegmentCard
import app.teadroid.ui.common.Single
import app.teadroid.ui.common.StateBadge
import app.teadroid.ui.common.navViewModel
import app.teadroid.ui.common.segmentShape
import app.teadroid.ui.common.timeAgo

class IssueVM(h: SavedStateHandle) : ViewModel() {
    val owner: String = h.get<String>("owner").orEmpty()
    val repo: String = h.get<String>("repo").orEmpty()
    val number: Int = h.get<Int>("number") ?: 0
    val isPull: Boolean = h.get<Boolean>("pull") ?: false
    private val api = Graph.api

    val bundle = Single<IssueBundle>(viewModelScope) { api.issueBundle(owner, repo, number, isPull) }

    var draft by mutableStateOf("")
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    init {
        bundle.load()
    }

    private fun act(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                block()
                bundle.load()
                if (success != null) message = success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Something went wrong"
            } finally {
                busy = false
            }
        }
    }

    fun postComment() {
        val text = draft.trim()
        if (text.isEmpty()) return
        act { api.addComment(owner, repo, number, text); draft = "" }
    }

    fun setState(state: String) = act(if (state == "closed") "Closed" else "Reopened") {
        api.setIssueState(owner, repo, number, state)
    }

    fun merge(style: String, deleteBranch: Boolean) = act("Merged") {
        api.mergePull(owner, repo, number, style, deleteBranch)
    }
}

@Composable
fun IssueScreen(nav: NavController) {
    val vm = navViewModel { IssueVM(it) }
    val data = vm.bundle.value
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var showMerge by remember { mutableStateOf(false) }

    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isPull) "Pull request #${vm.number}" else "Issue #${vm.number}", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { data?.issue?.htmlUrl?.let(uriHandler::openUri) }, enabled = data != null) {
                        Icon(Icons.Outlined.OpenInBrowser, "Open in browser")
                    }
                },
            )
        },
        bottomBar = {
            if (data != null) CommentBar(vm)
        },
    ) { pad ->
        RefreshBox(vm.bundle.loading && data != null, vm.bundle::load, Modifier.padding(pad)) {
            when {
                data == null && vm.bundle.error != null -> ErrorState(vm.bundle.error ?: "", vm.bundle::load)
                data == null -> LoadingBox()
                else -> {
                    val issue = data.issue
                    val pull = data.pull
                    val open = issue.state == "open"
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    issue.title,
                                    style = MaterialTheme.typography.headlineSmallEmphasized,
                                    fontWeight = FontWeight.Bold,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StateBadge(
                                        issue.state,
                                        merged = pull?.merged == true || issue.pullRequest?.merged == true,
                                        draft = pull?.draft == true,
                                        isPull = vm.isPull,
                                    )
                                    MetaText(
                                        "${issue.user.login} opened ${timeAgo(issue.createdAt)}",
                                        Modifier.weight(1f),
                                    )
                                }
                                if (issue.labels.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        issue.labels.forEach { LabelChip(it) }
                                    }
                                }
                                if (issue.assignees.isNotEmpty()) {
                                    MetaText("Assigned to " + issue.assignees.joinToString { it.login })
                                }
                                if (pull != null) PullInfo(pull.head.ref, pull.base.ref, pull.additions, pull.deletions, pull.changedFiles, pull.mergeable && open && !pull.merged)
                                ActionButtons(
                                    open = open,
                                    merged = pull?.merged == true,
                                    isPull = vm.isPull,
                                    canMerge = pull?.mergeable == true && open,
                                    busy = vm.busy,
                                    onToggle = { vm.setState(if (open) "closed" else "open") },
                                    onMerge = { showMerge = true },
                                )
                            }
                        }
                        item {
                            CommentCard(
                                Comment(body = issue.body, user = issue.user, createdAt = issue.createdAt),
                                segmentShape(0, data.comments.size + 1),
                                emptyText = "No description provided.",
                            )
                        }
                        data.comments.forEachIndexed { i, c ->
                            item { CommentCard(c, segmentShape(i + 1, data.comments.size + 1)) }
                        }
                    }
                }
            }
        }
    }

    if (showMerge) MergeDialog(vm, onDismiss = { showMerge = false })
}

@Composable
private fun PullInfo(head: String, base: String, additions: Int, deletions: Int, files: Int, mergeable: Boolean) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(head, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.Outlined.ArrowForward, null, Modifier.size(16.dp))
                Text(base, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("+$additions", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("−$deletions", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                MetaText("$files files changed")
            }
            if (mergeable) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    MetaText("No conflicts, ready to merge")
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    open: Boolean,
    merged: Boolean,
    isPull: Boolean,
    canMerge: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
    onMerge: () -> Unit,
) {
    if (merged) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isPull && canMerge) {
            Button(onClick = onMerge, enabled = !busy, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Outlined.MergeType, null, Modifier.size(18.dp))
                Text("  Merge")
            }
        }
        FilledTonalButton(onClick = onToggle, enabled = !busy, shapes = ButtonDefaults.shapes()) {
            Icon(if (open) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, Modifier.size(18.dp))
            Text(if (open) "  Close" else "  Reopen")
        }
        if (busy) LoadingIndicator(Modifier.size(32.dp))
    }
}

@Composable
private fun CommentCard(c: Comment, shape: androidx.compose.ui.graphics.Shape, emptyText: String? = null) {
    SegmentCard(shape = shape) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(c.user.avatarUrl, c.user.login, size = 32.dp)
            Column {
                Text(c.user.login, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                MetaText(timeAgo(c.createdAt))
            }
        }
        Column(Modifier.padding(top = 12.dp)) {
            if (c.body.isBlank()) {
                Text(emptyText.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Markdown(c.body)
            }
        }
    }
}

@Composable
private fun CommentBar(vm: IssueVM) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = vm.draft,
                onValueChange = { vm.draft = it },
                placeholder = { Text("Add a comment…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = vm::postComment,
                enabled = vm.draft.isNotBlank() && !vm.busy,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
        }
    }
}

@Composable
private fun MergeDialog(vm: IssueVM, onDismiss: () -> Unit) {
    val options = listOf("merge" to "Create merge commit", "squash" to "Squash and merge", "rebase" to "Rebase and merge")
    var selected by remember { mutableStateOf("merge") }
    var deleteBranch by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge pull request") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected == value, onClick = { selected = value }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == value, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().selectable(deleteBranch, onClick = { deleteBranch = !deleteBranch }, role = Role.Checkbox),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(deleteBranch, onCheckedChange = null)
                    Text("Delete branch afterwards", Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); vm.merge(selected, deleteBranch) }) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
