package app.teadroid.ui.issue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.teadroid.data.Graph
import app.teadroid.ui.common.Single
import app.teadroid.ui.common.navViewModel
import app.teadroid.ui.nav.Routes

class NewIssueVM(h: SavedStateHandle) : ViewModel() {
    val owner: String = h.get<String>("owner").orEmpty()
    val repo: String = h.get<String>("repo").orEmpty()
    private val api = Graph.api

    var title by mutableStateOf("")
    var body by mutableStateOf("")
    val selected = mutableStateListOf<Long>()
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    val labels = Single(viewModelScope) { api.labels(owner, repo) }

    init {
        labels.load()
    }

    fun submit(onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val issue = api.createIssue(owner, repo, title.trim(), body.trim(), selected.toList())
                onCreated(issue.number)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Couldn't create the issue"
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun NewIssueScreen(nav: NavController) {
    val vm = navViewModel { NewIssueVM(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New issue") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
            )
        },
        floatingActionButton = {
            if (vm.title.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!vm.busy) vm.submit { number ->
                            nav.popBackStack()
                            nav.navigate(Routes.issue(vm.owner, vm.repo, number, false))
                        }
                    },
                    icon = {
                        if (vm.busy) LoadingIndicator(Modifier.size(24.dp)) else Icon(Icons.Filled.Check, null)
                    },
                    text = { Text("Create") },
                )
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("${vm.owner}/${vm.repo}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = vm.title,
                onValueChange = { vm.title = it },
                label = { Text("Title") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.body,
                onValueChange = { vm.body = it },
                label = { Text("Description (Markdown)") },
                shape = RoundedCornerShape(20.dp),
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            val labels = vm.labels.value.orEmpty()
            if (labels.isNotEmpty()) {
                Text("Labels", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { label ->
                        FilterChip(
                            selected = label.id in vm.selected,
                            onClick = { if (label.id in vm.selected) vm.selected.remove(label.id) else vm.selected.add(label.id) },
                            label = { Text(label.name) },
                        )
                    }
                }
            }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 72.dp))
        }
    }
}
