package site.colebolebole.ezject.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.data.Notification
import site.colebolebole.ezject.ui.common.Dot
import site.colebolebole.ezject.ui.common.MetaText
import site.colebolebole.ezject.ui.common.Paged
import site.colebolebole.ezject.ui.common.PagedLazyColumn
import site.colebolebole.ezject.ui.common.SegmentCard
import site.colebolebole.ezject.ui.common.timeAgo
import site.colebolebole.ezject.ui.nav.Routes

class InboxVM : ViewModel() {
    private val api = Graph.api
    var showRead by mutableStateOf(false)
    var unread by mutableLongStateOf(0L)

    val list = Paged(viewModelScope) { p, l -> api.notifications(showRead, p, l) }

    fun refreshUnread() {
        viewModelScope.launch { runCatching { unread = api.unreadCount() } }
    }

    fun changeShowRead(value: Boolean) {
        showRead = value
        list.refresh()
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { api.markAllRead() }
            list.refresh()
            refreshUnread()
        }
    }

    fun markRead(n: Notification) {
        if (!n.unread) return
        viewModelScope.launch {
            runCatching { api.markRead(n.id) }
            refreshUnread()
        }
    }
}

private val subjectRe = Regex("""/repos/([^/]+)/([^/]+)/(issues|pulls)/(\d+)""")

@Composable
fun InboxTab(nav: NavController, vm: InboxVM) {
    LaunchedEffect(Unit) { vm.list.ensureLoaded() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Inbox") },
                actions = {
                    IconButton(onClick = vm::markAllRead) { Icon(Icons.Outlined.DoneAll, "Mark all as read") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        PagedLazyColumn(
            paged = vm.list,
            emptyIcon = Icons.Outlined.Inbox,
            emptyTitle = "Inbox zero",
            emptySubtitle = "You're all caught up.",
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            header = { toggles(vm) },
        ) { _, n, shape ->
            NotificationRow(n, shape) {
                vm.markRead(n)
                val m = subjectRe.find(n.subject.url)
                val fullName = n.repository?.fullName
                when {
                    m != null -> nav.navigate(
                        Routes.issue(m.groupValues[1], m.groupValues[2], m.groupValues[4].toInt(), m.groupValues[3] == "pulls"),
                    )
                    fullName != null -> nav.navigate(Routes.repo(fullName))
                }
            }
        }
    }
}

private fun LazyListScope.toggles(vm: InboxVM) {
    item {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !vm.showRead, onClick = { vm.changeShowRead(false) }, label = { Text("Unread") })
            FilterChip(selected = vm.showRead, onClick = { vm.changeShowRead(true) }, label = { Text("All") })
        }
    }
}

@Composable
private fun NotificationRow(n: Notification, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    val icon = when (n.subject.type) {
        "Issue" -> Icons.Outlined.ErrorOutline
        "Pull" -> Icons.Outlined.MergeType
        "Commit" -> Icons.Outlined.History
        "Repository" -> Icons.Outlined.Folder
        else -> Icons.Outlined.ChatBubbleOutline
    }
    SegmentCard(shape = shape, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    n.subject.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (n.unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaText("${n.repository?.fullName.orEmpty()} · ${timeAgo(n.updatedAt)}")
            }
            if (n.unread) Dot(MaterialTheme.colorScheme.primary, 10.dp)
        }
    }
}
