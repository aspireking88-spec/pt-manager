package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import com.example.printer.PrinterConnectionState
import com.example.ui.GenerateUiState
import com.example.ui.dialogs.VoucherDetailsPreviewDialog
import com.example.ui.theme.StatusGreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RuijiePackageItem
import com.example.data.model.VoucherPackage
import com.example.data.model.RuijieProject
import com.example.data.model.RuijieVoucherItem
import com.example.data.model.VoucherCodeType
import com.example.ui.PrinterV1ViewModel
import com.example.ui.UiState
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.RuijieBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer

/**
 * Generate Voucher Screen redesigned strictly according to Screen 2 of the Reference Image:
 * - Clean title
 * - User Group dropdown selector
 * - Profile (Package) dropdown selector
 * - Voucher Length segmented pills: [ 6 ] [ 7 ] [ 8 ] [ 9 ]
 * - Voucher Code Type cards: [ Alphanumeric ] [ Alphabetic ] [ Numeric ]
 * - Number of Vouchers: [ 10 ] [ 50 ] [ 100 ] [ 200 ] [ Custom ]
 * - Generate & Print and Generate action buttons
 */
@Composable
fun GenerateVoucherScreen(
    viewModel: PrinterV1ViewModel,
    onOpenProjectSelector: () -> Unit,
    onPreviewVouchers: (List<RuijieVoucherItem>) -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeColors = LocalAppThemeColors.current
    val activeAccount by viewModel.activeAccount.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val packagesState by viewModel.packagesState.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val generateQuantity by viewModel.generateQuantity.collectAsState()
    val generateRemark by viewModel.generateRemark.collectAsState()
    val voucherLength by viewModel.voucherLength.collectAsState()
    val voucherCodeType by viewModel.voucherCodeType.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isGenerateAndPrinting by viewModel.isGenerateAndPrinting.collectAsState()
    val generatedVouchers by viewModel.generatedVouchers.collectAsState()
    val generateError by viewModel.generateError.collectAsState()
    val generateUiState by viewModel.generateUiState.collectAsState()
    val printerState by viewModel.printerConnectionState.collectAsState()
    val isPrinterConnected = printerState is PrinterConnectionState.Connected

    var showGroupDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var voucherToPreview by remember { mutableStateOf<RuijieVoucherItem?>(null) }
    var voucherToDelete by remember { mutableStateOf<RuijieVoucherItem?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("generate_voucher_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Screen Header (Title + Printer Connection Status Icon on the same line)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Generate Voucher",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.testTag("generate_screen_diag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = if (isPrinterConnected) "PP583 Printer Connected" else "PP583 Printer Disconnected",
                        tint = if (isPrinterConnected) StatusGreen else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("generate_screen_printer_status_icon")
                    )
                }
            }
        }

        // Live Cloud Target Metadata Summary Card
        item {
            val effGid = selectedProject?.effectiveGidStr ?: activeAccount.selectedGroupIdStr
            val numGid = selectedProject?.effectiveId ?: activeAccount.selectedGroupId
            val tenant = selectedProject?.tenantName?.ifBlank { null } ?: activeAccount.tenantName?.ifBlank { null } ?: "default"
            val pkg = selectedPackage
            val userGid = pkg?.userGroupId?.toString()?.ifBlank { null } ?: pkg?.id?.toString() ?: "-"
            val authId = pkg?.authprofileid?.ifBlank { null } ?: pkg?.id?.toString() ?: "-"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target Project: ${selectedProject?.effectiveName ?: "Not selected"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tenant: $tenant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "Group ID: $effGid (Numeric: $numGid) | UserGroup: $userGid | Auth: $authId",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section 1: User Group (Screen 2)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "User Group",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGroupDialog = true }
                        .testTag("user_group_selector"),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedProject?.effectiveName ?: "Select User Group",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedProject != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select User Group",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Section 2: Profile (Package) (Screen 2)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Profile (Package)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileDialog = true }
                        .testTag("profile_package_selector"),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val profileText = selectedPackage?.packageName?.ifBlank { null }
                            ?: selectedPackage?.profileName?.ifBlank { null }
                            ?: "Select profile"

                        Text(
                            text = profileText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedPackage != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Profile",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // If loading profiles, show a subtle indicator
                if (packagesState is UiState.Loading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Loading Cloud packages...", style = MaterialTheme.typography.bodySmall, color = themeColors.secondaryTextColor)
                    }
                }
            }
        }

        // Section 3: Voucher Length (Screen 2: [6] [7] [8] [9])
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Voucher Length",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(6, 7, 8, 9).forEach { len ->
                        val isSelected = voucherLength == len
                        OutlinedButton(
                            onClick = { viewModel.setVoucherLength(len) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) RuijieBlue else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RuijieBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = "$len",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Voucher Code Type (Screen 2: 3 cards side by side)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Voucher Code Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoucherCodeType.values().forEach { type ->
                        val isSelected = voucherCodeType == type
                        NeumorphicCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("code_type_${type.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            elevation = if (isSelected) 1.dp else 2.5.dp,
                            containerColor = if (isSelected) RuijieBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            borderColor = if (isSelected) RuijieBlue else MaterialTheme.colorScheme.outlineVariant,
                            onClick = { viewModel.setVoucherCodeType(type) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) RuijieBlue else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = type.subtext,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = themeColors.secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (voucherCodeType != VoucherCodeType.ALPHANUMERIC) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ruijie Cloud API creates alphanumeric codes by default. Parameter mapping is sent to Cloud. Inspect API behavior in Settings > Diagnostics.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = themeColors.secondaryTextColor
                    )
                }
            }
        }

        // Section 5: Number of Vouchers
        // FIRST ROW: [ 10 ] [ 20 ] [ 30 ] [ 50 ] [ 100 ]
        // SECOND ROW: [ Custom Quantity ]
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Number of Vouchers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                // FIRST ROW: Preset quantity buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10, 20, 30, 50, 100).forEach { qty ->
                        val isSelected = generateQuantity == qty
                        OutlinedButton(
                            onClick = { viewModel.setGenerateQuantity(qty) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("qty_preset_$qty"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) RuijieBlue else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RuijieBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = "$qty",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var customQuantityText by remember {
                    mutableStateOf(if (generateQuantity in listOf(10, 20, 30, 50, 100) || generateQuantity > 1) generateQuantity.toString() else "")
                }
                LaunchedEffect(generateQuantity) {
                    if (generateQuantity in listOf(10, 20, 30, 50, 100)) {
                        customQuantityText = generateQuantity.toString()
                    }
                }

                // SECOND ROW: Single Custom Quantity input field
                OutlinedTextField(
                    value = customQuantityText,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        customQuantityText = digitsOnly
                        val parsed = digitsOnly.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            viewModel.setGenerateQuantity(parsed.coerceIn(1, 500))
                        } else if (digitsOnly.isEmpty()) {
                            viewModel.setGenerateQuantity(1)
                        }
                    },
                    label = { Text("Custom Quantity") },
                    placeholder = { Text("Enter quantity (e.g. 10)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_quantity_input")
                )
            }
        }

        // Status and Error State Banners
        when (val state = generateUiState) {
            is GenerateUiState.Validating -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            is GenerateUiState.CreationUnknown -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            is GenerateUiState.GroupNotSynchronized -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onOpenDiagnostics) {
                                    Text("Open Diagnostics", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            is GenerateUiState.ValidationError -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Validation Error (${state.missingField})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
            is GenerateUiState.PermissionError, is GenerateUiState.SessionExpired, is GenerateUiState.NetworkError -> {
                val errorMsg = when (state) {
                    is GenerateUiState.PermissionError -> state.message
                    is GenerateUiState.SessionExpired -> state.message
                    is GenerateUiState.NetworkError -> state.message
                    else -> generateError ?: "Generation error"
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            else -> {
                if (generateError != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = generateError ?: "Generation error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 7: Generate Action Buttons (Generate and Generate & Print)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val isBusy = isGenerating || isGenerateAndPrinting
            val canGenerate = !isBusy && selectedPackage != null && selectedProject != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 1: Generate Only (Ruijie Cloud API)
                OutlinedButton(
                    onClick = { viewModel.generateVouchers(autoPrint = false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("generate_vouchers_btn"),
                    enabled = canGenerate,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, if (canGenerate) RuijieBlue else MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RuijieBlue,
                        disabledContentColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = RuijieBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generating...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (canGenerate) RuijieBlue else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Action 2: Generate & Print (Ruijie Cloud API + Thermal Printer)
                Button(
                    onClick = { viewModel.generateVouchers(autoPrint = true) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("generate_and_print_btn"),
                    enabled = canGenerate,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RuijieBlue,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    if (isGenerateAndPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = if (canGenerate) Color.White else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate & Print",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 8: Generated Vouchers Display if present
        if (generatedVouchers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusGreenContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generated ${generatedVouchers.size} Voucher(s)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                            IconButton(onClick = { viewModel.clearGeneratedVouchers() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = StatusGreen)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.printBatch(generatedVouchers) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RuijieBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print All")
                            }

                            OutlinedButton(
                                onClick = {
                                    val text = generatedVouchers.joinToString("\n") { it.effectiveCode }
                                    viewModel.copyVoucherCode(text)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy All")
                            }
                        }
                    }
                }
            }

            items(generatedVouchers, key = { it.effectiveCode }) { voucher ->
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = voucher.effectiveCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = RuijieBlue,
                            modifier = Modifier.weight(1f)
                        )
                        // Action icons: [ Preview 👁 ] [ Delete 🗑 ] [ Print 🖨 ]
                        // STRICTLY ICONS ONLY. No text labels: Preview, Delete, Print.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { voucherToPreview = voucher },
                                modifier = Modifier.size(36.dp).testTag("preview_voucher_btn_${voucher.effectiveCode}")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = RuijieBlue, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = { voucherToDelete = voucher },
                                modifier = Modifier.size(36.dp).testTag("delete_voucher_btn_${voucher.effectiveCode}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                            }
                            IconButton(
                                onClick = { viewModel.printSingle(voucher) },
                                modifier = Modifier.size(36.dp).testTag("print_voucher_btn_${voucher.effectiveCode}")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Voucher Preview and Delete Dialogs
    voucherToPreview?.let { v ->
        androidx.compose.runtime.key(v.uuid ?: v.voucherCode ?: v.codeNo ?: v.effectiveCode) {
            VoucherDetailsPreviewDialog(
                voucher = v,
                selectedProject = selectedProject,
                activeAccount = activeAccount,
                onCopyCode = { viewModel.copyVoucherCode(it) },
                onPrint = {
                    viewModel.printSingle(v)
                    voucherToPreview = null
                },
                onUnbind = { voucher ->
                    viewModel.unbindVoucher(voucher) { success, _, updated ->
                        if (success) {
                            voucherToPreview = updated
                        }
                    }
                },
                onRefresh = { voucher ->
                    viewModel.refreshVoucherDetails(voucher) { updated ->
                        voucherToPreview = updated
                    }
                },
                onDismiss = { voucherToPreview = null }
            )
        }
    }

    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("Delete Voucher?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete voucher code \"${v.effectiveCode}\" from Ruijie Cloud?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = v
                        voucherToDelete = null
                        viewModel.deleteSingleVoucher(toDel) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = {
                Text("Select User Group (Project)", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (projects.isEmpty()) {
                        item {
                            Text(
                                "No Ruijie Cloud projects available. Tap Refresh or check Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.secondaryTextColor
                            )
                        }
                    } else {
                        items(projects, key = { it.effectiveId }) { proj ->
                            val isSel = selectedProject?.effectiveId == proj.effectiveId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectProject(proj)
                                        showGroupDialog = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) RuijieBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) RuijieBlue else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proj.effectiveName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) RuijieBlue else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!proj.tenantName.isNullOrBlank()) {
                                            Text(
                                                text = proj.tenantName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = themeColors.secondaryTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = RuijieBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Profile (Package) Selection Dialog
    if (showProfileDialog) {
        val packages = (packagesState as? UiState.Success)?.data ?: emptyList()

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text("Select Cloud Profile", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (packages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (packagesState is UiState.Loading) "Fetching Cloud profiles..." else "No profiles found in Ruijie Cloud.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.secondaryTextColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.loadPackages() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh Profiles")
                                }
                            }
                        }
                    } else {
                        items(packages, key = { it.effectiveId }) { pkg ->
                            val isSel = selectedPackage?.effectiveId == pkg.effectiveId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectPackage(pkg)
                                        showProfileDialog = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) RuijieBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) RuijieBlue else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pkg.effectiveName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) RuijieBlue else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Period: ${pkg.effectiveDuration} • Quota: ${pkg.effectiveQuotaFormatted ?: "Unlimited"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = themeColors.secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = RuijieBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
