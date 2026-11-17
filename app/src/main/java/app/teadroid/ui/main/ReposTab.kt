package app.teadroid.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.teadroid.data.Graph
import app.teadroid.ui.common.PagedLazyColumn
import app.teadroid.ui.common.Paged
import app.teadroid.ui.common.RepoRow
import app.teadroid.ui.nav.Routes

class ReposVM : ViewModel() {
    private val api = Graph.api
    var mode by mutableIntStateOf(0) // 0 = mine, 1 = starred
    val mine = Paged(viewModelScope) { p, l -> api.myRepos(p, l) }
    val starred = Paged(viewModelScope) { p, l -> api.starredRepos(p, l) }
    var creating by mutableStateOf(false)
    var createError by mutableStateOf<String?>(null)

    val current get() = if (mode == 0) mine else starred

    fun create(name: String, description: String, private: Boolean, onCreated: (owner: String, name: String) -> Unit) {
        viewModelScope.launch {
            creating = true
            createError = null
            try {
                val repo = api.createRepo(name.trim(), description.trim(), private)
                mine.refresh()
                onCreated(repo.owner.login, repo.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                createError = e.message
            } finally {
                creating = false
            }
        }
    }
}

@Composable
fun ReposTab(nav: NavController, vm: ReposVM) {
    LaunchedEffect(vm.mode) { vm.current.ensureLoaded() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Repositories") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Outlined.Search, "Search") }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New repo") },
            )
        },
    ) { pad ->
        PagedLazyColumn(
            paged = vm.current,
            emptyIcon = if (vm.mode == 0) Icons.Outlined.Folder else Icons.Outlined.StarBorder,
            emptyTitle = if (vm.mode == 0) "No repositories yet" else "Nothing starred",
            emptySubtitle = if (vm.mode == 0) "Create one with the button below." else "Star a repo to find it here.",
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            header = { filterChips(vm) },
        ) { _, repo, shape ->
            RepoRow(repo, shape) { nav.navigate(Routes.repo(repo.owner.login, repo.name)) }
        }
    }

    if (showCreate) {
        CreateRepoDialog(vm, onDismiss = { showCreate = false }) { owner, name ->
            showCreate = false
            nav.navigate(Routes.repo(owner, name))
        }
    }
}

private fun LazyListScope.filterChips(vm: ReposVM) {
    item {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.mode == 0, onClick = { vm.mode = 0 }, label = { Text("Mine") })
            FilterChip(selected = vm.mode == 1, onClick = { vm.mode = 1 }, label = { Text("Starred") })
        }
    }
}

@Composable
private fun CreateRepoDialog(vm: ReposVM, onDismiss: () -> Unit, onCreated: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var private by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!vm.creating) onDismiss() },
        title = { Text("New repository") },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Private", modifier = Modifier.weight(1f))
                    Switch(private, { private = it })
                }
                vm.createError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.create(name, description, private, onCreated) },
                enabled = name.isNotBlank() && !vm.creating,
            ) { Text(if (vm.creating) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !vm.creating) { Text("Cancel") } },
    )
}
