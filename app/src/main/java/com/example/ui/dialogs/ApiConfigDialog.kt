package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AuthMode
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieServer

@Composable
fun ApiConfigDialog(
    account: RuijieAccount,
    onDismiss: () -> Unit,
    onLogin: (username: String, secret: String, server: RuijieServer, customUrl: String, mode: AuthMode) -> Unit
) {
    var selectedAuthMode by remember { mutableStateOf(account.authMode) }
    var selectedServer by remember { mutableStateOf(account.server) }
    var username by remember {
        mutableStateOf(if (account.authMode == AuthMode.USER_ACCOUNT) account.username else account.appId)
    }
    var secret by remember { mutableStateOf(account.secret) }
    var customUrl by remember { mutableStateOf(account.customUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_api_config"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Ruijie Cloud Connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Auth Mode Tabs
                TabRow(
                    selectedTabIndex = if (selectedAuthMode == AuthMode.OPEN_API) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedAuthMode == AuthMode.OPEN_API,
                        onClick = { selectedAuthMode = AuthMode.OPEN_API },
                        text = { Text("Open API") }
                    )
                    Tab(
                        selected = selectedAuthMode == AuthMode.USER_ACCOUNT,
                        onClick = { selectedAuthMode = AuthMode.USER_ACCOUNT },
                        text = { Text("User Account") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Server Selection
                Text(
                    text = "Server Region",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuijieServer.values().forEach { s ->
                        val isSelected = selectedServer == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedServer = s }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedServer == RuijieServer.CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Custom Server Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (selectedAuthMode == AuthMode.OPEN_API) "App ID / Username" else "Username / Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_username"),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text(if (selectedAuthMode == AuthMode.OPEN_API) "App Secret" else "Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onLogin(username, secret, selectedServer, customUrl, selectedAuthMode)
                        },
                        modifier = Modifier.testTag("btn_save_login")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Text("Connect", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ApiConfigDialog(
    account: RuijieAccount,
    isLoggingIn: Boolean = false,
    loginError: String? = null,
    onLogin: (username: String, secret: String, server: RuijieServer, customUrl: String, mode: AuthMode) -> Unit,
    onLogout: (() -> Unit)? = null,
    onOpenSSO: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    ApiConfigDialog(
        account = account,
        onDismiss = onDismiss,
        onLogin = onLogin
    )
}
