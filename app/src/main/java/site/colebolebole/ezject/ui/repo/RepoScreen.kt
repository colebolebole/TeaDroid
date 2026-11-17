package site.colebolebole.ezject.ui.repo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import site.colebolebole.ezject.data.Branch
import site.colebolebole.ezject.data.Commit
import site.colebolebole.ezject.data.ContentEntry
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.data.Release
import site.colebolebole.ezject.data.Repo
import site.colebolebole.ezject.ui.common.ErrorState
import site.colebolebole.ezject.ui.common.IconMeta
import site.colebolebole.ezject.ui.common.IssueRow
import site.colebolebole.ezject.ui.common.LoadMoreEffect
import site.colebolebole.ezject.ui.common.LoadingBox
import site.colebolebole.ezject.ui.common.Markdown
import site.colebolebole.ezject.ui.common.MetaText
import site.colebolebole.ezject.ui.common.Paged
import site.colebolebole.ezject.ui.common.PullRow
import site.colebolebole.ezject.ui.common.RefreshBox
import site.colebolebole.ezject.ui.common.SegmentCard
import site.colebolebole.ezject.ui.common.Single
import site.colebolebole.ezject.ui.common.formatBytes
import site.colebolebole.ezject.ui.common.navViewModel
import site.colebolebole.ezject.ui.common.pagedItems
import site.colebolebole.ezject.ui.common.segmentShape
import site.colebolebole.ezject.ui.common.timeAgo
import site.colebolebole.ezject.ui.nav.Routes

data class DirListing(val entries: List<ContentEntry>, val readmeName: String?, val readme: String?)

class RepoVM(h: SavedStateHandle) : ViewModel() {
    val owner: String = h.get<String>("owner").orEmpty()
    val name: String = h.get<String>("repo").orEmpty()
    private val api = Graph.api

    val repo = Single(viewModelScope) { api.repo(owner, name) }
    var starred by mutableStateOf(false)
        private set

    /** null = the repo's default branch */
    var ref by mutableStateOf<String?>(null)
        private set
    var path by mutableStateOf("")
        private set

    var tab by mutableIntStateOf(0)
    var issueClosed by mutableStateOf(false)
    var pullClosed by mutableStateOf(false)

    val dir = Single(viewModelScope) { loadDir() }
    val branches = Single(viewModelScope) { api.branches(owner, name) }
    val issues = Paged(viewModelScope) { p, l -> api.repoIssues(owner, name, if (issueClosed) "closed" else "open", p, l) }
    val pulls = Paged(viewModelScope) { p, l -> api.repoPulls(owner, name, if (pullClosed) "closed" else "open", p, l) }
    val commits = Paged(viewModelScope) { p, l -> api.commits(owner, name, ref, p, l) }
    val releases = Paged(viewModelScope) { p, l -> api.releases(owner, name, p, l) }

    val currentRef: String get() = ref ?: repo.value?.defaultBranch.orEmpty()

    init {
        repo.load()
        dir.load()
        viewModelScope.launch { runCatching { starred = api.isStarred(owner, name) } }
    }

    private suspend fun loadDir(): DirListing {
        val entries = api.listDir(owner, name, path, ref)
            .sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
        val readme = entries.firstOrNull { !it.isDir && it.name.lowercase().let { n -> n == "readme" || n.startsWith("readme.") } }
        val text = readme?.let {
            runCatching { String(api.rawBytes(owner, name, it.path, ref), Charsets.UTF_8) }.getOrNull()
        }
        return DirListing(entries, readme?.name, text)
    }

    fun openDir(newPath: String) {
        path = newPath
        dir.load()
    }

    /** Returns true if it navigated up a directory. */
    fun up(): Boolean {
        if (path.isEmpty()) return false
        openDir(path.substringBeforeLast('/', ""))
        return true
    }

    fun selectBranch(branch: String) {
        ref = branch
        path = ""
        dir.load()
        commits.refresh()
    }

    fun toggleStar() {
        val target = !starred
        starred = target
        viewModelScope.launch {
            try {
                api.setStarred(owner, name, target)
                repo.load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                starred = !target
            }
        }
    }

    fun refreshAll() {
        repo.load(); dir.load()
        when (tab) {
            1 -> issues.refresh()
            2 -> pulls.refresh()
            3 -> commits.refresh()
            4 -> releases.refresh()
        }
    }
}

private val tabs = listOf("Code", "Issues", "Pull requests", "Commits", "Releases")

@Composable
fun RepoScreen(nav: NavController) {
    val vm = navViewModel { RepoVM(it) }
    val repo = vm.repo.value
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    BackHandler(enabled = vm.tab == 0 && vm.path.isNotEmpty()) { vm.up() }

    LaunchedEffect(vm.tab) {
        when (vm.tab) {
            1 -> vm.issues.ensureLoaded()
            2 -> vm.pulls.ensureLoaded()
            3 -> vm.commits.ensureLoaded()
            4 -> vm.releases.ensureLoaded()
        }
    }
    val activePaged: Paged<*>? = when (vm.tab) {
        1 -> vm.issues
        2 -> vm.pulls
        3 -> vm.commits
        4 -> vm.releases
        else -> null
    }
    if (activePaged != null) LoadMoreEffect(listState, activePaged)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(vm.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { repo?.htmlUrl?.let(uriHandler::openUri) }, enabled = repo != null) {
                        Icon(Icons.Outlined.OpenInBrowser, "Open in browser")
                    }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            if (vm.tab == 1 && repo?.hasIssues != false) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Routes.newIssue(vm.owner, vm.name)) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("New issue") },
                )
            }
        },
    ) { pad ->
        RefreshBox(vm.repo.loading && repo != null, vm::refreshAll, Modifier.padding(pad)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    when {
                        repo != null -> RepoHeader(vm, repo)
                        vm.repo.error != null -> ErrorState(vm.repo.error ?: "", vm.repo::load)
                        else -> LoadingBox()
                    }
                }
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        PrimaryScrollableTabRow(selectedTabIndex = vm.tab, edgePadding = 8.dp) {
                            tabs.forEachIndexed { i, title ->
                                Tab(selected = vm.tab == i, onClick = { vm.tab = i }, text = { Text(title) })
                            }
                        }
                    }
                }
                when (vm.tab) {
                    0 -> codeItems(vm, nav)
                    1 -> issueItems(vm, nav)
                    2 -> pullItems(vm, nav)
                    3 -> commitItems(vm)
                    4 -> releaseItems(vm)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun RepoHeader(vm: RepoVM, repo: Repo) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                repo.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (repo.private) Icon(Icons.Outlined.Lock, "Private", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (repo.description.isNotBlank()) {
            Text(repo.description, style = MaterialTheme.typography.bodyLarge)
        }
        if (repo.topics.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repo.topics.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconMeta(Icons.Outlined.StarBorder, "${repo.stars} stars")
            IconMeta(Icons.Outlined.CallSplit, "${repo.forks} forks")
            IconMeta(Icons.Outlined.Visibility, "${repo.watchers} watching")
            if (repo.language.isNotBlank()) MetaText(repo.language)
            if (repo.archived) MetaText("Archived")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (vm.starred) {
                Button(onClick = vm::toggleStar, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Filled.Star, null, Modifier.size(18.dp))
                    Text("  Starred")
                }
            } else {
                FilledTonalButton(onClick = vm::toggleStar, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.StarBorder, null, Modifier.size(18.dp))
                    Text("  Star")
                }
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(repo.cloneUrl)) },
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                Text("  Clone URL")
            }
        }
    }
}

// ------------------------------------------------------------------ code tab

private fun LazyListScope.codeItems(vm: RepoVM, nav: NavController) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BranchPicker(vm)
            if (vm.path.isNotEmpty()) {
                Text(
                    "/ " + vm.path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    val listing = vm.dir.value
    when {
        listing == null && vm.dir.error != null -> item { ErrorState(vm.dir.error ?: "", vm.dir::load) }
        listing == null -> item { LoadingBox() }
        listing.entries.isEmpty() -> item {
            site.colebolebole.ezject.ui.common.EmptyState(Icons.Outlined.Folder, "Nothing here yet")
        }
        else -> {
            val shown = if (vm.path.isNotEmpty()) listOf<ContentEntry?>(null) + listing.entries else listing.entries
            shown.forEachIndexed { i, entry ->
                item {
                    val shape = segmentShape(i, shown.size)
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (entry == null) {
                            EntryRow("..", true, null, shape) { vm.up() }
                        } else {
                            EntryRow(entry.name, entry.isDir, entry.size, shape) {
                                if (entry.isDir) vm.openDir(entry.path)
                                else nav.navigate(Routes.file(vm.owner, vm.name, vm.currentRef, entry.path))
                            }
                        }
                    }
                }
            }
            val readme = listing.readme
            if (readme != null) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        SegmentCard(shape = segmentShape(0, 1)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Text(listing.readmeName.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.padding(top = 12.dp)) {
                                val ref = vm.currentRef
                                val dir = vm.path
                                Markdown(readme, resolveUrl = { url -> resolveRepoUrl(url, vm.owner, vm.name, ref, dir) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Resolves README-relative links/images to raw API URLs. */
private fun resolveRepoUrl(url: String, owner: String, repo: String, ref: String, dir: String): String {
    if (url.contains("://") || url.startsWith("mailto:") || url.startsWith("#") || url.startsWith("data:")) return url
    val parts = ArrayDeque<String>()
    val base = if (url.startsWith("/")) "" else dir
    (base.split('/') + url.trimStart('/').substringBefore('#').split('/')).forEach {
        when (it) {
            "", "." -> {}
            ".." -> parts.removeLastOrNull()
            else -> parts.addLast(it)
        }
    }
    return Graph.api.rawUrl(owner, repo, parts.joinToString("/"), ref)
}

@Composable
private fun EntryRow(name: String, isDir: Boolean, size: Long?, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    SegmentCard(shape = shape, onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (isDir) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile, null,
                tint = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (size != null && !isDir) MetaText(formatBytes(size))
        }
    }
}

@Composable
private fun BranchPicker(vm: RepoVM) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        AssistChip(
            onClick = { expanded = true; vm.branches.ensureLoaded() },
            label = { Text(vm.currentRef.ifBlank { "branch" }) },
            leadingIcon = { Icon(Icons.Outlined.AccountTree, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val list: List<Branch> = vm.branches.value.orEmpty()
            if (list.isEmpty()) {
                DropdownMenuItem(text = { Text(vm.branches.error ?: "Loading…") }, onClick = {}, enabled = false)
            }
            list.forEach { b ->
                DropdownMenuItem(
                    text = { Text(b.name) },
                    onClick = { expanded = false; vm.selectBranch(b.name) },
                    trailingIcon = { if (b.name == vm.currentRef) Icon(Icons.Outlined.Check, null) },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ issues / pulls

private fun LazyListScope.issueItems(vm: RepoVM, nav: NavController) {
    item {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !vm.issueClosed, onClick = { vm.issueClosed = false; vm.issues.refresh() }, label = { Text("Open") })
            FilterChip(selected = vm.issueClosed, onClick = { vm.issueClosed = true; vm.issues.refresh() }, label = { Text("Closed") })
        }
    }
    pagedItems(vm.issues, Icons.Outlined.ErrorOutline, "No issues", "Nothing ${if (vm.issueClosed) "closed" else "open"} here.") { _, issue, shape ->
        Column(Modifier.padding(horizontal = 16.dp)) {
            IssueRow(issue, shape, showRepo = false) { nav.navigate(Routes.issue(vm.owner, vm.name, issue.number, false)) }
        }
    }
}

private fun LazyListScope.pullItems(vm: RepoVM, nav: NavController) {
    item {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !vm.pullClosed, onClick = { vm.pullClosed = false; vm.pulls.refresh() }, label = { Text("Open") })
            FilterChip(selected = vm.pullClosed, onClick = { vm.pullClosed = true; vm.pulls.refresh() }, label = { Text("Closed") })
        }
    }
    pagedItems(vm.pulls, Icons.Outlined.MergeType, "No pull requests", "Nothing ${if (vm.pullClosed) "closed" else "open"} here.") { _, pr, shape ->
        Column(Modifier.padding(horizontal = 16.dp)) {
            PullRow(pr, shape) { nav.navigate(Routes.issue(vm.owner, vm.name, pr.number, true)) }
        }
    }
}

// ------------------------------------------------------------------ commits / releases

private fun LazyListScope.commitItems(vm: RepoVM) {
    pagedItems(vm.commits, Icons.Outlined.History, "No commits") { _, c, shape ->
        Column(Modifier.padding(horizontal = 16.dp)) { CommitRow(c, shape) }
    }
}

@Composable
private fun CommitRow(c: Commit, shape: androidx.compose.ui.graphics.Shape) {
    val uriHandler = LocalUriHandler.current
    SegmentCard(shape = shape, onClick = { if (c.htmlUrl.isNotBlank()) uriHandler.openUri(c.htmlUrl) }) {
        Text(
            c.commit.message.lineSequence().firstOrNull().orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MetaText(
                "${c.author?.login?.ifBlank { null } ?: c.commit.author.name} · ${timeAgo(c.commit.author.date.ifBlank { c.created })}",
                Modifier.weight(1f),
            )
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Text(
                    c.sha.take(7),
                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                )
            }
        }
    }
}

private fun LazyListScope.releaseItems(vm: RepoVM) {
    pagedItems(vm.releases, Icons.Outlined.NewReleases, "No releases", "Tagged releases will appear here.") { _, r, shape ->
        Column(Modifier.padding(horizontal = 16.dp)) { ReleaseRow(r, shape) }
    }
}

@Composable
private fun ReleaseRow(r: Release, shape: androidx.compose.ui.graphics.Shape) {
    val uriHandler = LocalUriHandler.current
    SegmentCard(shape = shape, onClick = { if (r.htmlUrl.isNotBlank()) uriHandler.openUri(r.htmlUrl) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                r.name.ifBlank { r.tagName },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (r.prerelease) MetaText("Pre-release")
            if (r.draft) MetaText("Draft")
        }
        MetaText("${r.tagName} · ${timeAgo(r.publishedAt.ifBlank { r.createdAt })}")
        if (r.body.isNotBlank()) {
            Column(Modifier.padding(top = 8.dp)) { Markdown(r.body.take(600)) }
        }
        if (r.assets.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                r.assets.forEach { a ->
                    AssistChip(
                        onClick = { uriHandler.openUri(a.url) },
                        label = { Text("${a.name} · ${formatBytes(a.size)}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}
