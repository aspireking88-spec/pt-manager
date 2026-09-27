package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppAppearanceSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.RuijieAccount
import com.example.ui.PrinterV1ViewModel

@Composable
fun SettingsScreen(
    account: RuijieAccount,
    appearance: AppAppearanceSettings,
    onUpdateAppearance: (AppAppearanceSettings) -> Unit,
    onOpenApiConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings & Configuration",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ruijie Cloud Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Prominent logged-in account card (documented account-info fields)
        val isLoggedIn = !account.accessToken.isNullOrBlank() || !account.token.isNullOrBlank()
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isLoggedIn)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isLoggedIn) "Logged in" else "Not logged in",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLoggedIn)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isLoggedIn) {
                    val displayAccount = account.email?.ifBlank { null }
                        ?: account.username.ifBlank { null }
                        ?: account.accountName
                    AccountInfoRow("Account", displayAccount)
                    account.nickname?.ifBlank { null }?.let { AccountInfoRow("User name", it) }
                    account.userRole?.ifBlank { null }?.let { AccountInfoRow("Role", it) }
                    account.defaultTenant?.ifBlank { null }?.let { AccountInfoRow("Default tenant", it) }
                    account.tenantName?.ifBlank { null }?.let { AccountInfoRow("Tenant", it) }
                } else {
                    Text(
                        text = "Use \"Configure API / Account Credentials\" below to log in with your App ID + Secret.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Active Server: ${account.activeBaseUrl}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Authentication: ${account.authMode.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onOpenApiConfig,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_configure_api"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Key, contentDescription = null)
            Text(
                text = "Configure API / Account Credentials",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Appearance & Display",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Text(
                    text = "Dark Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Switch(
                checked = appearance.themeMode == AppThemeMode.DARK,
                onCheckedChange = { checked ->
                    val newMode = if (checked) AppThemeMode.DARK else AppThemeMode.LIGHT
                    onUpdateAppearance(appearance.copy(themeMode = newMode))
                },
                modifier = Modifier.testTag("switch_dark_mode")
            )
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: PrinterV1ViewModel,
    onOpenAccounts: () -> Unit = {},
    onOpenApiConfig: () -> Unit = {},
    onOpenProjectSelector: () -> Unit = {},
    onOpenPrintDesign: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenTypography: () -> Unit = {},
    onOpenSSO: () -> Unit = {},
    onOpenSessionDiagnostic: () -> Unit = {},
    onOpenVoucherInspector: () -> Unit = {}
) {
    val account by viewModel.activeAccount.collectAsState(initial = RuijieAccount())
    val appearance by viewModel.appearance.collectAsState(initial = AppAppearanceSettings())
    SettingsScreen(
        account = account ?: RuijieAccount(),
        appearance = appearance,
        onUpdateAppearance = { viewModel.settingsStore.updateAppearance(it) },
        onOpenApiConfig = onOpenApiConfig
    )
}
