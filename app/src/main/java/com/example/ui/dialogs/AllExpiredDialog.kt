package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RuijieVoucherItem
import com.example.ui.components.LocalNeumorphicColors
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.NeumorphicDialog
import com.example.ui.components.NeumorphicOutlinedButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AllExpiredDialog(
    allVouchers: List<RuijieVoucherItem>,
    onDismiss: () -> Unit,
    onConfirmDelete: (matchingVouchers: List<RuijieVoucherItem>) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalNeumorphicColors.current

    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val displayDate = remember(selectedCalendar) { dateFormat.format(selectedCalendar.time) }

    // Start of the chosen date in milliseconds. Vouchers created BEFORE this timestamp are matched.
    val startOfChosenDayMillis = remember(selectedCalendar) {
        val cal = selectedCalendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // Calculate expired vouchers created strictly BEFORE the chosen date from real Ruijie Cloud data
    val matchingVouchers = remember(allVouchers, startOfChosenDayMillis) {
        allVouchers.filter { it.isExpiredCreatedBefore(startOfChosenDayMillis) }
    }

    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Mandatory confirmation dialog before deletion
    if (showConfirmationDialog) {
        NeumorphicDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = "Confirm Deletion",
            titleIcon = Icons.Default.Delete,
            buttons = {
                NeumorphicOutlinedButton(
                    onClick = { showConfirmationDialog = false },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }

                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onDismiss()
                        onConfirmDelete(matchingVouchers)
                    },
                    modifier = Modifier.testTag("confirm_delete_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        ) {
            Text(
                text = "You are about to delete ${matchingVouchers.size} expired voucher(s).\nThis action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
        }
    }

    NeumorphicDialog(
        onDismissRequest = onDismiss,
        title = "Delete Expired",
        titleIcon = Icons.Default.CalendarToday,
        buttons = {
            NeumorphicOutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_all_expired_dialog_btn")
            ) {
                Text("Cancel", color = colors.textSecondary)
            }

            Button(
                onClick = { showConfirmationDialog = true },
                enabled = matchingVouchers.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("delete_expired_matching_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete (${matchingVouchers.size})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "All expired vouchers created before $displayDate will be deleted from Ruijie Cloud.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            // Date selector neumorphic card
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val year = selectedCalendar.get(Calendar.YEAR)
                        val month = selectedCalendar.get(Calendar.MONTH)
                        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedCalendar = newCal
                            },
                            year,
                            month,
                            day
                        ).show()
                    },
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Chosen Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }

                    NeumorphicButton(
                        onClick = {
                            val year = selectedCalendar.get(Calendar.YEAR)
                            val month = selectedCalendar.get(Calendar.MONTH)
                            val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedCalendar = newCal
                                },
                                year,
                                month,
                                day
                            ).show()
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("select_date_button"),
                        primary = true,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Select Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Calculated Results Card with actual cloud count
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (matchingVouchers.isNotEmpty()) {
                            "Found ${matchingVouchers.size} expired voucher(s) created before $displayDate."
                        } else {
                            "No expired vouchers found created before $displayDate."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (matchingVouchers.isNotEmpty()) MaterialTheme.colorScheme.error else colors.textPrimary
                    )
                    Text(
                        text = "Only EXPIRED vouchers created before $displayDate will be deleted. Active and used vouchers will NOT be affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

