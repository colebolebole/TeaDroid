package app.teadroid.ui.user

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.teadroid.data.Graph
import app.teadroid.data.User
import app.teadroid.ui.common.CookieAvatar
import app.teadroid.ui.common.ErrorState
import app.teadroid.ui.common.LoadMoreEffect
import app.teadroid.ui.common.LoadingBox
import app.teadroid.ui.common.Paged
import app.teadroid.ui.common.RefreshBox
import app.teadroid.ui.common.RepoRow
import app.teadroid.ui.common.SectionHeader
import app.teadroid.ui.common.SegmentCard
import app.teadroid.ui.common.Single
import app.teadroid.ui.common.navViewModel
import app.teadroid.ui.common.pagedItems
import app.teadroid.ui.common.segmentShape
import app.teadroid.ui.nav.Routes
import app.teadroid.ui.nav.signOutToLogin

class UserVM(val name: String, val isMe: Boolean) : ViewModel() {
    private val api = Graph.api
    val user = Single(viewModelScope) { if (isMe) api.me().also { Graph.session.saveUser(it) } else api.user(name) }
    val repos = Paged(viewModelScope) { p, l -> if (isMe) api.myRepos(p, l) else api.userRepos(name, p, l) }

    fun ensureLoaded() {
        user.ensureLoaded(); repos.ensureLoaded()
    }

    fun refresh() {
        user.load(); repos.refresh()
    }
}

@Composable
fun UserScreen(nav: NavController, name: String, isMe: Boolean) {
    val vm = navViewModel(key = "user:$name:$isMe") { UserVM(name, isMe) }
    LaunchedEffect(Unit) { vm.ensureLoaded() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, vm.repos)
    var confirmSignOut by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(if (isMe) "You" else name) },
                navigationIcon = {
                    if (!isMe) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isMe) IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Outlined.Settings, "Settings")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        val user = vm.user.value
        RefreshBox(vm.user.loading && user != null, vm::refresh, Modifier.padding(pad)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when {
                    user != null -> item { ProfileHeader(user) }
                    vm.user.error != null -> item { ErrorState(vm.user.error ?: "", vm.user::load) }
                    else -> item { LoadingBox() }
                }
                if (isMe) {
                    item { SectionHeader("Account") }
                    item {
                        ActionRow(Icons.Outlined.Settings, "Settings", "Appearance, server, about", segmentShape(0, 2)) {
                            nav.navigate(Routes.SETTINGS)
                        }
                    }
                    item {
                        ActionRow(
                            Icons.AutoMirrored.Outlined.Logout, "Sign out", "Remove this account from the device",
                            segmentShape(1, 2), destructive = true,
                        ) { confirmSignOut = true }
                    }
                }
                item { SectionHeader("Repositories") }
                pagedItems(
                    vm.repos,
                    emptyIcon = Icons.Outlined.Folder,
                    emptyTitle = "No repositories",
                ) { _, repo, shape ->
                    RepoRow(repo, shape) { nav.navigate(Routes.repo(repo.owner.login, repo.name)) }
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("The access token stored on this device will be removed. Tokens created on the server stay valid until you revoke them.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; nav.signOutToLogin() }) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileHeader(user: User) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CookieAvatar(user.avatarUrl, user.login, size = 112.dp)
        Text(user.displayName, style = MaterialTheme.typography.headlineMediumEmphasized, fontWeight = FontWeight.Bold)
        if (user.fullName.isNotBlank()) {
            Text("@${user.login}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (user.description.isNotBlank()) {
            Text(user.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            if (user.location.isNotBlank()) AssistChip(onClick = {}, label = { Text(user.location) }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null, Modifier.size(18.dp)) })
            if (user.website.isNotBlank()) AssistChip(onClick = {}, label = { Text(user.website.removePrefix("https://").removePrefix("http://")) }, leadingIcon = { Icon(Icons.Outlined.Language, null, Modifier.size(18.dp)) })
            if (user.created.isNotBlank()) AssistChip(onClick = {}, label = { Text("Joined ${user.created.take(10)}") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp)) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cs = MaterialTheme.colorScheme
            Stat("Followers", user.followers, cs.primaryContainer, cs.onPrimaryContainer, Modifier.weight(1f))
            Stat("Following", user.following, cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f))
            Stat("Starred", user.starredRepos, cs.tertiaryContainer, cs.onTertiaryContainer, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, container: Color, content: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = container, contentColor = content) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLargeEmphasized, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    shape: androidx.compose.ui.graphics.Shape,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    SegmentCard(shape = shape, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
