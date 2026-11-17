package site.colebolebole.ezject.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.ui.common.IssueRow
import site.colebolebole.ezject.ui.common.Paged
import site.colebolebole.ezject.ui.common.PagedLazyColumn
import site.colebolebole.ezject.ui.nav.Routes

private val scopes = listOf(
    "Assigned" to "assigned",
    "Created" to "created",
    "Mentioned" to "mentioned",
    "Review requested" to "review_requested",
)

class IssuesVM : ViewModel() {
    private val api = Graph.api
    var scopeIndex by mutableIntStateOf(0)
    var kind by mutableIntStateOf(0)   // 0 = issues, 1 = pulls
    var closed by mutableStateOf(false)

    val list = Paged(viewModelScope) { p, l ->
        api.searchIssues(if (kind == 0) "issues" else "pulls", if (closed) "closed" else "open", scopes[scopeIndex].second, p, l)
    }

    fun update(block: () -> Unit) {
        block()
        list.refresh()
    }
}

@Composable
fun IssuesTab(nav: NavController, vm: IssuesVM) {
    LaunchedEffect(Unit) { vm.list.ensureLoaded() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Issues & PRs") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, "Search") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        PagedLazyColumn(
            paged = vm.list,
            emptyIcon = Icons.Outlined.TaskAlt,
            emptyTitle = "All clear",
            emptySubtitle = "Nothing matches these filters.",
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            header = { filters(vm) },
        ) { _, issue, shape ->
            IssueRow(issue, shape, showRepo = true) {
                val repo = issue.repository
                if (repo != null) nav.navigate(Routes.issue(repo.owner, repo.name, issue.number, issue.isPull))
            }
        }
    }
}

private fun LazyListScope.filters(vm: IssuesVM) {
    item {
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Issues", "Pull requests").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = vm.kind == i,
                        onClick = { vm.update { vm.kind = i } },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scopes.forEachIndexed { i, (label, _) ->
                    FilterChip(
                        selected = vm.scopeIndex == i,
                        onClick = { vm.update { vm.scopeIndex = i } },
                        label = { Text(label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !vm.closed, onClick = { vm.update { vm.closed = false } }, label = { Text("Open") })
                FilterChip(selected = vm.closed, onClick = { vm.update { vm.closed = true } }, label = { Text("Closed") })
            }
        }
    }
}
