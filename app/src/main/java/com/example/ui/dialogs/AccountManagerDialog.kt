package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.RuijieAccount
import com.example.data.model.RuijieServer
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicButtonStyle
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.components.NeumorphicTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagerDialog(
    accounts: List<RuijieAccount>,
    activeAccount: RuijieAccount,
    onSelectAccount: (String) -> Unit,
    onAddAccount: (name: String, appId: String, secret: String, server: RuijieServer, customUrl: String) -> Unit,
    onUpdateAccount: (RuijieAccount) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingAccount by remember { mutableStateOf<RuijieAccount?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val colors = LocalNeumorphicColors.current

    if (isAddingNew || editingAccount != null) {
        val currentEdit = editingAccount
        AccountEditDialog(
            initialAccount = currentEdit,
            onSave = { name, appId, secret, server, customUrl ->
                if (currentEdit != null) {
                    onUpdateAccount(
                        currentEdit.copy(
                            accountName = name,
                            appId = appId,
                            secret = secret,
                            server = server,
                            customServerUrl = customUrl,
                            accessToken = if (server != currentEdit.server || customUrl != currentEdit.customServerUrl) "" else currentEdit.accessToken
                        )
                    )
                } else {
                    onAddAccount(name, appId, secret, server, customUrl)
                }
                isAddingNew = false
                editingAccount = null
            },
            onDismiss = {
                isAddingNew = false
                editingAccount = null
            }
        )
        return
    }

    NeumorphicDialog(
        title = "Account Manager",
        subtitle = "Manage multiple Ruijie Cloud accounts",
        icon = Icons.Default.ManageAccounts,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("account_manager_dialog")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tokens and servers are strictly isolated per account.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    val isActive = account.id == activeAccount.id
                    NeumorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!isActive) {
                                    onSelectAccount(account.id)
                                }
                            }
                            .testTag("account_card_${account.id}"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = if (isActive) 1.5.dp else 2.dp,
                        containerColor = if (isActive) colors.primary.copy(alpha = 0.08f) else colors.surface,
                        borderColor = if (isActive) colors.primary else colors.border
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = { onSelectAccount(account.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = account.accountName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = colors.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Server: ${account.server.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                if (!account.username.isNullOrBlank() || !account.email.isNullOrBlank()) {
                                    Text(
                                        text = "User: ${if (!account.username.isNullOrBlank()) account.username else account.email}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                if (!account.tenantName.isNullOrBlank()) {
                                    Text(
                                        text = "Org: ${account.tenantName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                if (!account.selectedGroupName.isNullOrBlank()) {
                                    Text(
                                        text = "Project: ${account.selectedGroupName} (${account.selectedGroupId})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                NeumorphicIconButton(
                                    onClick = { editingAccount = account },
                                    icon = Icons.Default.Edit,
                                    contentDescription = "Edit Account",
                                    size = 32.dp
                                )
                                if (accounts.size > 1) {
                                    NeumorphicIconButton(
                                        onClick = { onDeleteAccount(account.id) },
                                        icon = Icons.Default.Delete,
                                        contentDescription = "Delete Account",
                                        tint = MaterialTheme.colorScheme.error,
                                        size = 32.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    onClick = onDismiss,
                    style = NeumorphicButtonStyle.Outlined,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", color = colors.textSecondary)
                }

                NeumorphicButton(
                    onClick = { isAddingNew = true },
                    primary = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("add_account_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Account", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditDialog(
    initialAccount: RuijieAccount?,
    onSave: (name: String, appId: String, secret: String, server: RuijieServer, customUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialAccount?.accountName ?: "Account") }
    var appId by remember { mutableStateOf(initialAccount?.appId ?: "") }
    var secret by remember { mutableStateOf(initialAccount?.secret ?: "") }
    var selectedServer by remember { mutableStateOf(initialAccount?.server ?: RuijieServer.ASIA) }
    var customUrl by remember { mutableStateOf(initialAccount?.customServerUrl ?: "") }
    var serverExpanded by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }

    val colors = LocalNeumorphicColors.current

    NeumorphicDialog(
        title = if (initialAccount == null) "Add Account" else "Edit Account",
        subtitle = if (initialAccount == null) "Configure new Ruijie Cloud account" else initialAccount.accountName,
        icon = Icons.Default.ManageAccounts,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeumorphicTextField(
                value = name,
                onValueChange = { name = it },
                labelText = "Account Name",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Server Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Server Region",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { serverExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedServer.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = serverExpanded,
                        onDismissRequest = { serverExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        RuijieServer.entries.forEach { server ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(server.displayName, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                        if (server.baseUrl.isNotBlank()) {
                                            Text(
                                                server.baseUrl,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                },
                                leadingIcon = if (selectedServer == server) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary) }
                                } else null,
                                onClick = {
                                    selectedServer = server
                                    serverExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedServer == RuijieServer.CUSTOM) {
                NeumorphicTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    labelText = "Custom Server URL",
                    placeholder = { Text("https://my-ruijie-cloud.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            NeumorphicTextField(
                value = appId,
                onValueChange = { appId = it },
                labelText = "App ID",
                placeholder = { Text("e.g. open6dfe7fa50c37") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            NeumorphicTextField(
                value = secret,
                onValueChange = { secret = it },
                labelText = "App Secret",
                trailingIcon = {
                    IconButton(onClick = { secretVisible = !secretVisible }) {
                        Icon(
                            imageVector = if (secretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (secretVisible) "Hide Secret" else "Show Secret",
                            tint = colors.textSecondary
                        )
                    }
                },
                visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    onClick = onDismiss,
                    style = NeumorphicButtonStyle.Outlined,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }

                NeumorphicButton(
                    onClick = {
                        onSave(name, appId, secret, selectedServer, customUrl)
                    },
                    primary = true,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

