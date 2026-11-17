package site.colebolebole.ezject.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import site.colebolebole.ezject.data.DEFAULT_SERVER
import site.colebolebole.ezject.data.Graph
import site.colebolebole.ezject.ui.common.navViewModel

class LoginVM : ViewModel() {
    var server by mutableStateOf(Graph.session.serverUrl.ifBlank { DEFAULT_SERVER })
    var mode by mutableIntStateOf(0) // 0 = token, 1 = username + password
    var token by mutableStateOf("")
    var user by mutableStateOf("")
    var pass by mutableStateOf("")
    var otp by mutableStateOf("")
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val canSubmit: Boolean
        get() = server.isNotBlank() && !busy &&
            if (mode == 0) token.isNotBlank() else user.isNotBlank() && pass.isNotBlank()

    fun submit(onDone: () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val base = normalizeServer(server)
                val api = Graph.api
                val tok = if (mode == 0) token.trim() else api.createToken(base, user.trim(), pass, otp.trim().ifBlank { null })
                val me = api.verify(base, tok)
                Graph.session.signIn(base, tok, me)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Couldn't sign in"
            } finally {
                busy = false
            }
        }
    }

    /** Accepts "host", "https://host/", or a pasted ".../api/swagger" style URL. */
    private fun normalizeServer(raw: String): String {
        var s = raw.trim()
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
        s = s.replace(Regex("/api(/.*)?$"), "").trimEnd('/')
        return s
    }
}

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val vm = navViewModel { LoginVM() }
    var showSecret by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Bolt, null,
                    Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "EZject",
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Your Gitea, in your pocket",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.server,
                        onValueChange = { vm.server = it },
                        label = { Text("Server URL") },
                        leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Access token", "Password").forEachIndexed { i, label ->
                            SegmentedButton(
                                selected = vm.mode == i,
                                onClick = { vm.mode = i },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                                label = { Text(label) },
                            )
                        }
                    }

                    if (vm.mode == 0) {
                        OutlinedTextField(
                            value = vm.token,
                            onValueChange = { vm.token = it },
                            label = { Text("Access token") },
                            leadingIcon = { Icon(Icons.Outlined.Key, null) },
                            trailingIcon = { VisibilityToggle(showSecret) { showSecret = it } },
                            singleLine = true,
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Settings → Applications → Generate New Token") },
                        )
                    } else {
                        OutlinedTextField(
                            value = vm.user,
                            onValueChange = { vm.user = it },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Outlined.Person, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = vm.pass,
                            onValueChange = { vm.pass = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            trailingIcon = { VisibilityToggle(showSecret) { showSecret = it } },
                            singleLine = true,
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = vm.otp,
                            onValueChange = { vm.otp = it.filter(Char::isDigit).take(8) },
                            label = { Text("2FA code (if enabled)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("A token is created for this device; your password isn't stored.") },
                        )
                    }

                    vm.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                        )
                    }

                    Button(
                        onClick = { vm.submit(onLoggedIn) },
                        enabled = vm.canSubmit,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        if (vm.busy) {
                            LoadingIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Sign in", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityToggle(visible: Boolean, onChange: (Boolean) -> Unit) {
    IconButton(onClick = { onChange(!visible) }) {
        Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Toggle visibility")
    }
}
