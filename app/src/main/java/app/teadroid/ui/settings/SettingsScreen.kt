package app.teadroid.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.teadroid.data.Graph
import app.teadroid.ui.common.SectionHeader
import app.teadroid.ui.common.SegmentCard
import app.teadroid.ui.common.Single
import app.teadroid.ui.common.navViewModel
import app.teadroid.ui.common.segmentShape

class SettingsVM : ViewModel() {
    val version = Single(viewModelScope) { Graph.api.version() }
}

@Composable
fun SettingsScreen(nav: NavController) {
    val vm = navViewModel { SettingsVM() }
    LaunchedEffect(Unit) { vm.version.ensureLoaded() }
    val session = Graph.session
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SectionHeader("Appearance")
            SegmentCard(shape = segmentShape(0, 2)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = session.themeMode == value,
                            onClick = { session.updateThemeMode(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            label = { Text(label) },
                        )
                    }
                }
            }
            val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SegmentCard(shape = segmentShape(1, 2)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Dynamic color", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (dynamicAvailable) "Use colors from your wallpaper" else "Requires Android 12 or newer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = session.dynamicColor && dynamicAvailable,
                        onCheckedChange = { session.updateDynamicColor(it) },
                        enabled = dynamicAvailable,
                    )
                }
            }

            SectionHeader("Server")
            SegmentCard(shape = segmentShape(0, 3)) {
                Text("URL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(session.serverUrl, style = MaterialTheme.typography.bodyLarge)
            }
            SegmentCard(shape = segmentShape(1, 3)) {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("@${session.username}", style = MaterialTheme.typography.bodyLarge)
            }
            SegmentCard(shape = segmentShape(2, 3)) {
                Text("Gitea version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    vm.version.value ?: vm.version.error ?: "Loading…",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            SectionHeader("About")
            SegmentCard(shape = segmentShape(0, 1)) {
                Text("TeaDroid", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A Material 3 Expressive client for Gitea.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
