package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SalonService
import com.example.ui.components.DigitalTicketCard
import com.example.ui.components.ServiceCard
import com.example.ui.components.ShopOpenBadge
import com.example.ui.components.SyncStatusIndicator
import com.example.ui.theme.SalonAmberAccent
import com.example.ui.theme.SalonDarkCardBorder
import com.example.ui.theme.SalonDarkCardSurface
import com.example.ui.theme.SalonDarkSubCardBorder
import com.example.ui.theme.SalonDarkSubCardSurface
import com.example.ui.theme.SalonDarkNavyBackground
import com.example.ui.theme.SalonGoldLight
import com.example.ui.theme.SalonGoldPrimary
import com.example.ui.theme.StatusCancelledRed
import com.example.ui.theme.StatusRejoinedCyan
import com.example.ui.theme.StatusServingGreen
import com.example.ui.theme.StatusWaitingAmber
import com.example.ui.theme.TextGrayMuted
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.viewmodel.SalonUiState
import com.example.ui.viewmodel.SalonViewModel

@Composable
fun CustomerScreen(
    viewModel: SalonViewModel,
    uiState: SalonUiState,
    modifier: Modifier = Modifier
) {
    val nameInput by viewModel.customerNameInput.collectAsState()
    val phoneInput by viewModel.customerPhoneInput.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val notesInput by viewModel.customerNotesInput.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SalonDarkNavyBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Banner & Salon Info
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                border = BorderStroke(1.dp, SalonDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_salon_hero),
                        contentDescription = "Salon Interior Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xCC111214),
                                        Color(0xFF111214)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "STUDENT SALON",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SalonGoldLight,
                                letterSpacing = 1.sp
                            )
                            ShopOpenBadge(isOpen = uiState.shopConfig.isOpen)
                        }

                        Text(
                            text = "Premium Men's Grooming • Telo, Chandrapura, Bokaro, Jharkhand",
                            fontSize = 11.sp,
                            color = TextGraySecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyncStatusIndicator(syncStatus = uiState.syncStatus)

                if (uiState.myTicketsInfoList.size > 1) {
                    Surface(
                        color = SalonDarkSubCardSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SalonDarkSubCardBorder)
                    ) {
                        Text(
                            text = "🎫 ${uiState.myTicketsInfoList.size} Active Tickets",
                            fontSize = 10.sp,
                            color = SalonGoldLight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Active Tickets Section (if user has any saved tickets)
        if (uiState.myTicketsInfoList.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.myTicketsInfoList.size == 1) {
                        Text(
                            text = "YOUR ACTIVE QUEUE TICKET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SalonGoldPrimary,
                            letterSpacing = 1.2.sp
                        )

                        val singleInfo = uiState.myTicketsInfoList.first()
                        DigitalTicketCard(
                            ticketInfo = singleInfo,
                            onCancelTicket = { ticketId -> viewModel.cancelMyTicket(ticketId) },
                            onClearTicket = { viewModel.clearMySavedTicket(singleInfo.ticket.id) }
                        )
                    } else {
                        // Multi-Ticket Header & Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE TICKETS (${uiState.myTicketsInfoList.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SalonGoldPrimary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Tap ticket to switch",
                                fontSize = 11.sp,
                                color = TextGraySecondary
                            )
                        }

                        // Horizontal Switcher Chips
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.myTicketsInfoList.size) { index ->
                                val info = uiState.myTicketsInfoList[index]
                                val isSelected = info.ticket.id == (uiState.selectedTicketId ?: uiState.myTicketsInfoList.first().ticket.id)
                                val isServing = info.ticket.status == com.example.data.model.QueueStatus.SERVING

                                Surface(
                                    onClick = { viewModel.selectTicket(info.ticket.id) },
                                    color = if (isSelected) Color(0x33F59E0B) else SalonDarkSubCardSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) SalonGoldPrimary else SalonDarkSubCardBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isServing) StatusServingGreen else SalonGoldPrimary
                                                )
                                        )
                                        Text(
                                            text = "#${info.ticket.queueNumber} ${info.ticket.customerName}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextGraySecondary
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = "• Active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SalonGoldLight
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Focused Selected Ticket Card
                        val focusedTicket = uiState.myTicketInfo ?: uiState.myTicketsInfoList.first()
                        DigitalTicketCard(
                            ticketInfo = focusedTicket,
                            onCancelTicket = { ticketId -> viewModel.cancelMyTicket(ticketId) },
                            onClearTicket = { viewModel.clearMySavedTicket(focusedTicket.ticket.id) }
                        )
                    }
                }
            }
        }

        // Join Queue Form (ALWAYS visible for joining queue or creating additional tickets)
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
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Join",
                            tint = SalonGoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "JOIN DIGITAL QUEUE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhitePrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Get your digital queue ticket from your phone and skip waiting in line!",
                        fontSize = 12.sp,
                        color = TextGraySecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    // Customer Name Input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.customerNameInput.value = it },
                        label = { Text("Your Full Name *") },
                        placeholder = { Text("e.g., Yunus, Rahul, Aman") },
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
                            .testTag("customer_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer Phone Input (optional)
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { viewModel.customerPhoneInput.value = it },
                        label = { Text("Mobile Number (Optional)") },
                        placeholder = { Text("e.g., 98765 43210") },
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
                            .testTag("customer_phone_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT SERVICE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextGraySecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Services List
                    SalonService.entries.forEach { service ->
                        ServiceCard(
                            service = service,
                            isSelected = selectedService == service,
                            onSelect = { viewModel.selectedService.value = service },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Join Button (ALWAYS visible, disabled only when shop is closed or name is blank)
                    Button(
                        onClick = { viewModel.joinQueue() },
                        enabled = uiState.shopConfig.isOpen && nameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SalonGoldPrimary,
                            disabledContainerColor = SalonDarkCardBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("join_queue_submit_button")
                    ) {
                        Text(
                            text = if (uiState.shopConfig.isOpen) "🎫 Get Digital Queue Ticket" else "🔴 Salon Currently Closed",
                            color = if (uiState.shopConfig.isOpen && nameInput.isNotBlank()) Color.Black else TextGrayMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (!uiState.shopConfig.isOpen) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Owner has currently closed the queue. Please check back during opening hours.",
                            fontSize = 11.sp,
                            color = StatusCancelledRed,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Live Queue Snapshot
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(StatusServingGreen, CircleShape)
                            )
                            Text(
                                text = "LIVE QUEUE STATUS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhitePrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "${uiState.waitingTickets.size} in line",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SalonAmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Currently Serving Bar
                    Surface(
                        color = SalonDarkSubCardSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SalonDarkSubCardBorder),
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
                                Text(
                                    text = "CURRENTLY IN CHAIR",
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGrayMuted
                                )
                                Text(
                                    text = if (uiState.servingTicket != null)
                                        "#${uiState.servingTicket.queueNumber} - ${uiState.servingTicket.customerName}"
                                    else "Chair Available",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.servingTicket != null) StatusServingGreen else SalonGoldLight
                                )
                            }

                            if (uiState.servingTicket != null) {
                                Text(
                                    text = "${uiState.servingTicket.service.iconEmoji} ${uiState.servingTicket.service.title}",
                                    fontSize = 12.sp,
                                    color = TextGraySecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Next In line
                    if (uiState.nextCustomerInLine != null) {
                        Surface(
                            color = SalonDarkSubCardSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SalonDarkSubCardBorder),
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
                                    Text(
                                        text = "NEXT CUSTOMER",
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGrayMuted
                                    )
                                    Text(
                                        text = "#${uiState.nextCustomerInLine.queueNumber} - ${uiState.nextCustomerInLine.customerName}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusRejoinedCyan
                                    )
                                }

                                Text(
                                    text = "${uiState.nextCustomerInLine.service.iconEmoji} ${uiState.nextCustomerInLine.service.title}",
                                    fontSize = 12.sp,
                                    color = TextGraySecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Salon Info & Location Details
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
                        text = "LOCATION & TIMINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextGraySecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = SalonGoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Student Salon",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextWhitePrimary
                            )
                            Text(
                                text = "Telo, Chandrapura, Bokaro, Jharkhand",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Hours",
                            tint = StatusServingGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Daily: 08:00 AM - 09:00 PM",
                            fontSize = 12.sp,
                            color = TextGraySecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contact",
                            tint = StatusRejoinedCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "+91 91234 56789",
                            fontSize = 12.sp,
                            color = TextGraySecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
