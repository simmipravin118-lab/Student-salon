package com.example.ui.screens.owner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.ui.components.ServingChairCard
import com.example.ui.components.ShopOpenBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SalonAmberAccent
import com.example.ui.theme.SalonDarkCardBorder
import com.example.ui.theme.SalonDarkCardSurface
import com.example.ui.theme.SalonDarkSubCardBorder
import com.example.ui.theme.SalonDarkSubCardSurface
import com.example.ui.theme.SalonDarkNavyBackground
import com.example.ui.theme.SalonGoldLight
import com.example.ui.theme.SalonGoldPrimary
import com.example.ui.theme.StatusCancelledRed
import com.example.ui.theme.StatusCompletedBlue
import com.example.ui.theme.StatusRejoinedCyan
import com.example.ui.theme.StatusServingGreen
import com.example.ui.theme.StatusSkippedOrange
import com.example.ui.theme.StatusWaitingAmber
import com.example.ui.theme.TextGrayMuted
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.viewmodel.SalonUiState
import com.example.ui.viewmodel.SalonViewModel

@Composable
fun OwnerDashboardScreen(
    viewModel: SalonViewModel,
    uiState: SalonUiState,
    modifier: Modifier = Modifier
) {
    if (!uiState.isOwnerAuthenticated) {
        OwnerLoginScreen(
            onLogin = { pin -> viewModel.authenticateOwner(pin) },
            modifier = modifier
        )
    } else {
        OwnerDashboardContent(
            viewModel = viewModel,
            uiState = uiState,
            modifier = modifier
        )
    }
}

@Composable
fun OwnerLoginScreen(
    onLogin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SalonDarkNavyBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
            border = BorderStroke(1.5.dp, SalonGoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_login_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SalonGoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Owner Security",
                        tint = SalonGoldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "STUDENT SALON",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = SalonGoldLight,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Owner Management Login",
                    fontSize = 13.sp,
                    color = TextGraySecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter Owner PIN") },
                    placeholder = { Text("Default: 1234") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SalonGoldPrimary,
                        unfocusedBorderColor = SalonDarkCardBorder,
                        focusedLabelColor = SalonGoldLight,
                        cursorColor = SalonGoldPrimary,
                        focusedTextColor = TextWhitePrimary,
                        unfocusedTextColor = TextWhitePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_pin_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = StatusCancelledRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val success = onLogin(pin)
                        if (!success) {
                            errorMessage = "Incorrect PIN. (Default: 1234)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SalonGoldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("owner_login_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Unlock Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        pin = "1234"
                        onLogin("1234")
                    }
                ) {
                    Text(text = "Quick Demo Unlock (PIN: 1234)", color = SalonAmberAccent, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun OwnerDashboardContent(
    viewModel: SalonViewModel,
    uiState: SalonUiState,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SalonDarkNavyBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Owner Header & Shop Open/Close Controller
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                border = BorderStroke(1.dp, SalonDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STUDENT SALON DASHBOARD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = SalonGoldLight,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Owner Control Center",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logoutOwner() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SalonDarkNavyBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = SalonGoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shop Open/Closed Toggle Control
                    Surface(
                        color = if (uiState.shopConfig.isOpen) StatusServingGreen.copy(alpha = 0.15f) else StatusCancelledRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (uiState.shopConfig.isOpen) StatusServingGreen else StatusCancelledRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.shopConfig.isOpen) "🟢 SALON IS OPEN" else "🔴 SALON IS CLOSED",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (uiState.shopConfig.isOpen) StatusServingGreen else StatusCancelledRed
                                )
                                Text(
                                    text = if (uiState.shopConfig.isOpen) "Customers can join digital queue" else "Queue intake is currently paused",
                                    fontSize = 11.sp,
                                    color = TextGraySecondary
                                )
                            }

                            Switch(
                                checked = uiState.shopConfig.isOpen,
                                onCheckedChange = { viewModel.toggleShopStatus(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StatusServingGreen,
                                    uncheckedThumbColor = TextGraySecondary,
                                    uncheckedTrackColor = SalonDarkCardBorder
                                ),
                                modifier = Modifier.testTag("shop_open_switch")
                            )
                        }
                    }
                }
            }
        }

        // Active Serving Chair Card
        item {
            ServingChairCard(
                servingTicket = uiState.servingTicket,
                currentTime = uiState.currentTimeMillis,
                onCompleteService = { viewModel.completeService(it) },
                onSkipCustomer = { viewModel.skipCustomer(it) },
                onStartNextCustomer = { viewModel.startNextCustomer() }
            )
        }

        // Next Customer Preview Card
        if (uiState.nextCustomerInLine != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                    border = BorderStroke(1.dp, StatusRejoinedCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "NEXT CUSTOMER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRejoinedCyan,
                                    letterSpacing = 1.sp
                                )
                                if (uiState.nextCustomerInLine.isRejoinedPriority) {
                                    StatusBadge(status = QueueStatus.REJOINED)
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "#${uiState.nextCustomerInLine.queueNumber} - ${uiState.nextCustomerInLine.customerName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhitePrimary
                            )

                            Text(
                                text = "${uiState.nextCustomerInLine.service.iconEmoji} ${uiState.nextCustomerInLine.service.title} (${uiState.nextCustomerInLine.service.durationMinutes} min)",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }

                        Button(
                            onClick = { viewModel.startCustomer(uiState.nextCustomerInLine.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = SalonGoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("start_next_in_line_button")
                        ) {
                            Text("▶ Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Waiting Queue Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WAITING QUEUE (${uiState.waitingTickets.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhitePrimary,
                        letterSpacing = 1.sp
                    )

                    if (uiState.waitingTickets.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.startNextCustomer() },
                            colors = ButtonDefaults.buttonColors(containerColor = SalonGoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("owner_start_next_button")
                        ) {
                            Text("▶ Start Next", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.waitingTickets.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No customers currently waiting in queue.",
                            fontSize = 13.sp,
                            color = TextGraySecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                }
            }
        }

        // Waiting Customers List
        items(uiState.waitingTickets, key = { it.id }) { ticket ->
            val position = uiState.waitingTickets.indexOf(ticket) + 1
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ticket.isRejoinedPriority) StatusRejoinedCyan.copy(alpha = 0.08f) else SalonDarkCardSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (ticket.isRejoinedPriority) StatusRejoinedCyan else SalonDarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("waiting_ticket_row_${ticket.queueNumber}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (ticket.isRejoinedPriority) StatusRejoinedCyan else SalonDarkNavyBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$position",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (ticket.isRejoinedPriority) Color.Black else SalonGoldPrimary
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "#${ticket.queueNumber} - ${ticket.customerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextWhitePrimary
                                )
                                if (ticket.isRejoinedPriority) {
                                    StatusBadge(status = QueueStatus.REJOINED)
                                }
                            }

                            Text(
                                text = "${ticket.service.iconEmoji} ${ticket.service.title} • ${ticket.service.durationMinutes} min",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.skipCustomer(ticket.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip",
                                tint = StatusSkippedOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Button(
                            onClick = { viewModel.startCustomer(ticket.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusServingGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("▶ Start", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Skipped Customers Section (Rejoin Queue Priority)
        if (uiState.skippedTickets.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SKIPPED CUSTOMERS (${uiState.skippedTickets.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusSkippedOrange,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Customer missed their turn? Click Rejoin to give them priority for the next available turn.",
                        fontSize = 11.sp,
                        color = TextGraySecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                }
            }

            items(uiState.skippedTickets, key = { it.id }) { ticket ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                    border = BorderStroke(1.dp, StatusSkippedOrange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "#${ticket.queueNumber} - ${ticket.customerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextWhitePrimary
                                )
                                StatusBadge(status = QueueStatus.SKIPPED)
                            }
                            Text(
                                text = "${ticket.service.iconEmoji} ${ticket.service.title}",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }

                        Button(
                            onClick = { viewModel.rejoinCustomer(ticket.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejoinedCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("rejoin_queue_button_${ticket.queueNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Rejoin",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("↩ Rejoin", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Daily Summary & Analytics Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                border = BorderStroke(1.dp, SalonDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TODAY'S SUMMARY & ANALYTICS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhitePrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val summary = uiState.dailySummary

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricBox(title = "TOTAL", count = "${summary.totalCustomers}", color = SalonGoldLight, modifier = Modifier.weight(1f))
                        SummaryMetricBox(title = "WAITING", count = "${summary.waitingCount}", color = StatusWaitingAmber, modifier = Modifier.weight(1f))
                        SummaryMetricBox(title = "SERVING", count = "${summary.servingCount}", color = StatusServingGreen, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricBox(title = "COMPLETED", count = "${summary.completedCount}", color = StatusCompletedBlue, modifier = Modifier.weight(1f))
                        SummaryMetricBox(title = "SKIPPED", count = "${summary.skippedCount}", color = StatusSkippedOrange, modifier = Modifier.weight(1f))
                        SummaryMetricBox(title = "CANCELLED", count = "${summary.cancelledCount}", color = StatusCancelledRed, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick Admin Actions (Reset, Demo Data)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                border = BorderStroke(1.dp, SalonDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MANAGEMENT CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextGraySecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.seedDemoQueue() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SalonGoldPrimary),
                            border = BorderStroke(1.dp, SalonGoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seed_demo_queue_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Demo", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Demo Queue", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCancelledRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_today_queue_button")
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Day Queue", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Queue for Today?") },
            text = { Text("This will clear all tickets for today and restart queue numbering at #1. This is usually done at the start of a new business day.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetTodayQueue()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCancelledRed)
                ) {
                    Text("Yes, Reset Queue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricBox(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SalonDarkSubCardSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SalonDarkSubCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextGrayMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
