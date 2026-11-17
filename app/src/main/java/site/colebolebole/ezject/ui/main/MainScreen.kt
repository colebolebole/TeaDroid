package site.colebolebole.ezject.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.ui.common.navViewModel
import site.colebolebole.ezject.ui.user.UserScreen

enum class MainTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Repos("Repos", Icons.Outlined.Folder, Icons.Filled.Folder),
    Issues("Issues", Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt),
    Inbox("Inbox", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    You("You", Icons.Outlined.Person, Icons.Filled.Person),
}

@Composable
fun MainScreen(nav: NavController) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val home = navViewModel { HomeVM() }
    val repos = navViewModel { ReposVM() }
    val issues = navViewModel { IssuesVM() }
    val inbox = navViewModel { InboxVM() }

    LaunchedEffect(Unit) { inbox.refreshUnread() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            ShortNavigationBar {
                MainTab.entries.forEachIndexed { i, tab ->
                    ShortNavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = {
                            val icon = if (selected == i) tab.selectedIcon else tab.icon
                            if (tab == MainTab.Inbox && inbox.unread > 0) {
                                BadgedBox(badge = { Badge { Text(if (inbox.unread > 99) "99+" else inbox.unread.toString()) } }) {
                                    Icon(icon, tab.label)
                                }
                            } else {
                                Icon(icon, tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).consumeWindowInsets(inner)) {
            when (MainTab.entries[selected]) {
                MainTab.Home -> HomeTab(nav, home) { selected = it.ordinal }
                MainTab.Repos -> ReposTab(nav, repos)
                MainTab.Issues -> IssuesTab(nav, issues)
                MainTab.Inbox -> InboxTab(nav, inbox)
                MainTab.You -> UserScreen(nav, name = Graph.session.username, isMe = true)
            }
        }
    }
}
