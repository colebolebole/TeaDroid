package app.teadroid.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import app.teadroid.data.Graph
import app.teadroid.ui.common.Paged
import app.teadroid.ui.common.PagedLazyColumn
import app.teadroid.ui.common.RepoRow
import app.teadroid.ui.common.UserRow
import app.teadroid.ui.common.navViewModel
import app.teadroid.ui.nav.Routes

class SearchVM : ViewModel() {
    private val api = Graph.api
    var query by mutableStateOf("")
    var mode by mutableIntStateOf(0) // 0 = repositories, 1 = users

    val repos = Paged(viewModelScope) { p, l -> api.searchRepos(query.trim(), p, l) }
    val users = Paged(viewModelScope) { p, l -> api.searchUsers(query.trim(), p, l) }

    init {
        viewModelScope.launch {
            snapshotFlow { query.trim() to mode }
                .debounce(300)
                .collectLatest { (_, m) -> if (m == 0) repos.refresh() else users.refresh() }
        }
    }
}

@Composable
fun SearchScreen(nav: NavController) {
    val vm = navViewModel { SearchVM() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                    OutlinedTextField(
                        value = vm.query,
                        onValueChange = { vm.query = it },
                        placeholder = { Text(if (vm.mode == 0) "Search repositories" else "Search users") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (vm.query.isNotEmpty()) IconButton(onClick = { vm.query = "" }) { Icon(Icons.Outlined.Clear, "Clear") }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    listOf("Repositories", "Users").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = vm.mode == i,
                            onClick = { vm.mode = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        val contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
        if (vm.mode == 0) {
            PagedLazyColumn(
                paged = vm.repos,
                emptyIcon = Icons.Outlined.Folder,
                emptyTitle = "No repositories found",
                modifier = Modifier.padding(pad),
                contentPadding = contentPadding,
            ) { _, repo, shape ->
                RepoRow(repo, shape) { nav.navigate(Routes.repo(repo.owner.login, repo.name)) }
            }
        } else {
            PagedLazyColumn(
                paged = vm.users,
                emptyIcon = Icons.Outlined.Person,
                emptyTitle = "No users found",
                modifier = Modifier.padding(pad),
                contentPadding = contentPadding,
            ) { _, user, shape ->
                UserRow(user, shape) { nav.navigate(Routes.user(user.login)) }
            }
        }
    }
}
