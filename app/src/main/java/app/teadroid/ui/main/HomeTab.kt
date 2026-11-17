package app.teadroid.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.teadroid.data.Activity
import app.teadroid.data.Graph
import app.teadroid.data.ListResult
import app.teadroid.data.Repo
import app.teadroid.ui.common.Avatar
import app.teadroid.ui.common.CookieAvatar
import app.teadroid.ui.common.IconMeta
import app.teadroid.ui.common.LoadMoreEffect
import app.teadroid.ui.common.MetaText
import app.teadroid.ui.common.Paged
import app.teadroid.ui.common.RefreshBox
import app.teadroid.ui.common.SectionHeader
import app.teadroid.ui.common.SegmentCard
import app.teadroid.ui.common.Single
import app.teadroid.ui.common.pagedItems
import app.teadroid.ui.common.timeAgo
import app.teadroid.ui.nav.Routes

class HomeVM : ViewModel() {
    private val api = Graph.api

    val repos = Single(viewModelScope) { api.myRepos(1, 50) }
    val assigned = Single(viewModelScope) { api.searchIssues("issues", "open", "assigned", 1, 1).total ?: 0 }
    val reviews = Single(viewModelScope) { api.searchIssues("pulls", "open", "review_requested", 1, 1).total ?: 0 }
    val unread = Single(viewModelScope) { api.unreadCount() }
    val feed = Paged(viewModelScope) { page, limit -> api.feeds(Graph.session.username, page, limit) }

    fun ensureLoaded() {
        repos.ensureLoaded(); assigned.ensureLoaded(); reviews.ensureLoaded(); unread.ensureLoaded(); feed.ensureLoaded()
    }

    fun refresh() {
        repos.load(); assigned.load(); reviews.load(); unread.load(); feed.refresh()
    }

    val recentRepos: List<Repo>
        get() = repos.value?.items.orEmpty().sortedByDescending { it.updatedAt }.take(10)
}

@Composable
fun HomeTab(nav: NavController, vm: HomeVM, onSelectTab: (MainTab) -> Unit) {
    LaunchedEffect(Unit) { vm.ensureLoaded() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, vm.feed)
    val session = Graph.session

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, "Search") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        RefreshBox(vm.feed.loading && vm.feed.loaded, vm::refresh, Modifier.padding(pad)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CookieAvatar(session.avatarUrl, session.username, 64.dp)
                        Column {
                            Text("Welcome back", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                session.displayName.ifBlank { session.username },
                                style = MaterialTheme.typography.headlineMediumEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val cs = MaterialTheme.colorScheme
                        StatTile(
                            "Repos", vm.repos.value?.total?.toString() ?: "–", Icons.Outlined.Folder,
                            cs.primaryContainer, cs.onPrimaryContainer, Modifier.weight(1f),
                        ) { onSelectTab(MainTab.Repos) }
                        StatTile(
                            "Assigned", vm.assigned.value?.toString() ?: "–", Icons.Outlined.TaskAlt,
                            cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f),
                        ) { onSelectTab(MainTab.Issues) }
                        StatTile(
                            "Unread", vm.unread.value?.toString() ?: "–", Icons.Outlined.Notifications,
                            cs.tertiaryContainer, cs.onTertiaryContainer, Modifier.weight(1f),
                        ) { onSelectTab(MainTab.Inbox) }
                    }
                }
                val recent = vm.recentRepos
                if (recent.isNotEmpty()) {
                    item { SectionHeader("Recently updated") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recent) { repo -> RecentRepoCard(repo) { nav.navigate(Routes.repo(repo.owner.login, repo.name)) } }
                        }
                    }
                }
                item { SectionHeader("Activity") }
                pagedItems(
                    vm.feed,
                    emptyIcon = Icons.Outlined.Timeline,
                    emptyTitle = "No activity yet",
                    emptySubtitle = "Push a commit or open an issue and it'll show up here.",
                ) { _, activity, shape ->
                    ActivityRow(activity, shape) { target -> nav.navigate(target) }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = content,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineMediumEmphasized, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecentRepoCard(repo: Repo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(220.dp).height(132.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MetaText(repo.description.ifBlank { repo.owner.login }, maxLines = 2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconMeta(Icons.Outlined.StarBorder, repo.stars.toString())
                MetaText(timeAgo(repo.updatedAt))
            }
        }
    }
}

// ------------------------------------------------------------------ activity feed

private data class ActivityView(val verb: String, val icon: ImageVector, val detail: String?, val route: String?)

private fun describe(a: Activity): ActivityView {
    val repo = a.repo
    val full = repo?.fullName
    val repoRoute = full?.let { Routes.repo(it) }

    fun issueRoute(content: String, pull: Boolean): String? {
        val n = content.substringBefore('|').trim().toIntOrNull() ?: return repoRoute
        return if (repo != null) Routes.issue(repo.owner.login, repo.name, n, pull) else null
    }
    fun issueDetail(content: String): String? {
        val n = content.substringBefore('|').trim()
        val rest = content.substringAfter('|', "").lineSequence().firstOrNull().orEmpty().trim()
        return if (n.isBlank()) null else "#$n${if (rest.isNotBlank()) " $rest" else ""}"
    }

    return when (a.opType) {
        "create_repo" -> ActivityView("created repository", Icons.Outlined.AddCircleOutline, null, repoRoute)
        "rename_repo" -> ActivityView("renamed repository", Icons.Outlined.Edit, a.content, repoRoute)
        "star_repo" -> ActivityView("starred", Icons.Outlined.Star, null, repoRoute)
        "watch_repo" -> ActivityView("started watching", Icons.Outlined.Notifications, null, repoRoute)
        "transfer_repo" -> ActivityView("transferred", Icons.Outlined.Folder, null, repoRoute)
        "commit_repo", "mirror_sync_push" -> {
            val branch = a.refName.removePrefix("refs/heads/")
            val (count, msg) = runCatching {
                val o = Graph.api.json.parseToJsonElement(a.content).jsonObject
                val commits = o["Commits"]?.jsonArray
                val len = o["Len"]?.jsonPrimitive?.content?.toIntOrNull() ?: commits?.size ?: 0
                val first = (commits?.firstOrNull() as? JsonObject)?.get("Message")?.jsonPrimitive?.content.orEmpty()
                len to first.lineSequence().firstOrNull().orEmpty()
            }.getOrDefault(0 to "")
            val extra = if (count > 1) " (+${count - 1} more)" else ""
            ActivityView(
                "pushed to $branch at", Icons.Outlined.History,
                (msg + extra).ifBlank { null }, repoRoute,
            )
        }
        "create_issue" -> ActivityView("opened issue in", Icons.Outlined.ErrorOutline, issueDetail(a.content), issueRoute(a.content, false))
        "comment_issue" -> ActivityView("commented on", Icons.Outlined.ChatBubbleOutline, issueDetail(a.content), issueRoute(a.content, false))
        "close_issue" -> ActivityView("closed issue in", Icons.Outlined.CheckCircle, issueDetail(a.content), issueRoute(a.content, false))
        "reopen_issue" -> ActivityView("reopened issue in", Icons.Outlined.ErrorOutline, issueDetail(a.content), issueRoute(a.content, false))
        "create_pull_request" -> ActivityView("opened pull request in", Icons.Outlined.MergeType, issueDetail(a.content), issueRoute(a.content, true))
        "comment_pull" -> ActivityView("commented on pull request in", Icons.Outlined.ChatBubbleOutline, issueDetail(a.content), issueRoute(a.content, true))
        "merge_pull_request", "auto_merge_pull_request" -> ActivityView("merged pull request in", Icons.Outlined.MergeType, issueDetail(a.content), issueRoute(a.content, true))
        "close_pull_request" -> ActivityView("closed pull request in", Icons.Outlined.CheckCircle, issueDetail(a.content), issueRoute(a.content, true))
        "reopen_pull_request" -> ActivityView("reopened pull request in", Icons.Outlined.MergeType, issueDetail(a.content), issueRoute(a.content, true))
        "approve_pull_request" -> ActivityView("approved pull request in", Icons.Outlined.RateReview, issueDetail(a.content), issueRoute(a.content, true))
        "reject_pull_request" -> ActivityView("requested changes in", Icons.Outlined.RateReview, issueDetail(a.content), issueRoute(a.content, true))
        "pull_request_ready_for_review" -> ActivityView("marked ready for review in", Icons.Outlined.RateReview, issueDetail(a.content), issueRoute(a.content, true))
        "push_tag" -> ActivityView("pushed tag ${a.refName.removePrefix("refs/tags/")} to", Icons.Outlined.LocalOffer, null, repoRoute)
        "delete_tag" -> ActivityView("deleted tag ${a.refName.removePrefix("refs/tags/")} from", Icons.Outlined.Delete, null, repoRoute)
        "delete_branch" -> ActivityView("deleted branch ${a.refName.removePrefix("refs/heads/")} from", Icons.Outlined.Delete, null, repoRoute)
        "publish_release" -> ActivityView("released ${a.content} in", Icons.Outlined.NewReleases, null, repoRoute)
        else -> ActivityView(a.opType.replace('_', ' '), Icons.Outlined.Timeline, null, repoRoute)
    }
}

@Composable
private fun ActivityRow(a: Activity, shape: androidx.compose.ui.graphics.Shape, onNavigate: (String) -> Unit) {
    val v = describe(a)
    SegmentCard(shape = shape, onClick = v.route?.let { r -> { onNavigate(r) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(contentAlignment = Alignment.Center) {
                Avatar(a.actUser.avatarUrl, a.actUser.login, size = 40.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    buildActivityTitle(a, v),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                v.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(v.icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    MetaText(timeAgo(a.created))
                }
            }
        }
    }
}

@Composable
private fun buildActivityTitle(a: Activity, v: ActivityView) = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
    append(a.actUser.login)
    pop()
    append(" ${v.verb}")
    a.repo?.let {
        append(" ")
        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.SemiBold))
        append(it.fullName)
        pop()
    }
}
