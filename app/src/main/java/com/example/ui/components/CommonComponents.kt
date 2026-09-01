package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerTicketInfo
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
import com.example.data.model.SalonService
import com.example.data.model.ShopConfig
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
import kotlin.math.max

@Composable
fun StatusBadge(
    status: QueueStatus,
    isPriority: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (status) {
        QueueStatus.SERVING -> Triple(StatusServingGreen.copy(alpha = 0.12f), StatusServingGreen.copy(alpha = 0.35f), StatusServingGreen)
        QueueStatus.WAITING -> if (isPriority) Triple(StatusRejoinedCyan.copy(alpha = 0.12f), StatusRejoinedCyan.copy(alpha = 0.35f), StatusRejoinedCyan)
                               else Triple(SalonGoldPrimary.copy(alpha = 0.12f), SalonGoldPrimary.copy(alpha = 0.35f), SalonGoldPrimary)
        QueueStatus.COMPLETED -> Triple(StatusCompletedBlue.copy(alpha = 0.12f), StatusCompletedBlue.copy(alpha = 0.35f), StatusCompletedBlue)
        QueueStatus.SKIPPED -> Triple(StatusSkippedOrange.copy(alpha = 0.12f), StatusSkippedOrange.copy(alpha = 0.35f), StatusSkippedOrange)
        QueueStatus.CANCELLED -> Triple(StatusCancelledRed.copy(alpha = 0.12f), StatusCancelledRed.copy(alpha = 0.35f), StatusCancelledRed)
        QueueStatus.REJOINED -> Triple(StatusRejoinedCyan.copy(alpha = 0.12f), StatusRejoinedCyan.copy(alpha = 0.35f), StatusRejoinedCyan)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isPriority && status == QueueStatus.WAITING) "↩ Priority" else "${status.emoji} ${status.displayName}",
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun ShopOpenBadge(isOpen: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = SalonDarkSubCardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SalonDarkSubCardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(if (isOpen) scale else 1f)
                    .background(
                        if (isOpen) StatusServingGreen else StatusCancelledRed,
                        shape = CircleShape
                    )
            )
            Text(
                text = if (isOpen) "OPEN" else "CLOSED",
                color = if (isOpen) StatusServingGreen else StatusCancelledRed,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun SyncStatusIndicator(
    syncStatus: com.example.data.model.SyncStatus,
    modifier: Modifier = Modifier
) {
    val isLive = syncStatus.isCloudConnected && !syncStatus.isUsingLocalCache
    Surface(
        color = if (isLive) SalonDarkSubCardSurface.copy(alpha = 0.8f) else Color(0x33B45309),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isLive) SalonDarkSubCardBorder.copy(alpha = 0.7f) else SalonGoldPrimary.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isLive) StatusServingGreen else SalonGoldPrimary,
                        shape = CircleShape
                    )
            )
            Text(
                text = if (isLive) "Live Cloud Synced" else "Offline Mode • Showing Local Cache",
                color = if (isLive) TextGraySecondary else SalonGoldLight,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun ServiceCard(
    service: SalonService,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SalonDarkSubCardSurface else SalonDarkCardSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) SalonGoldPrimary else SalonDarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelect() }
            .testTag("service_card_${service.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) SalonGoldPrimary else SalonDarkSubCardSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.iconEmoji,
                    fontSize = 20.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isSelected) SalonGoldLight else TextWhitePrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = service.description,
                    fontSize = 11.sp,
                    color = TextGraySecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Duration",
                        tint = if (isSelected) SalonGoldPrimary else TextGrayMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${service.durationMinutes} mins",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) SalonGoldPrimary else TextGrayMuted
                    )
                }
            }
        }
    }
}

@Composable
fun DigitalTicketCard(
    ticketInfo: CustomerTicketInfo,
    onCancelTicket: (String) -> Unit,
    onClearTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ticket = ticketInfo.ticket
    var showCancelDialog by remember { mutableStateOf(false) }

    val isServing = ticket.status == QueueStatus.SERVING
    val isWaiting = ticket.status == QueueStatus.WAITING
    val isCompleted = ticket.status == QueueStatus.COMPLETED

    val infiniteTransition = rememberInfiniteTransition(label = "turnPulsing")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = SalonDarkCardSurface
        ),
        border = BorderStroke(
            width = if (isServing) 2.dp else 1.dp,
            color = if (isServing) StatusServingGreen.copy(alpha = glowAlpha) else SalonDarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_queue_ticket_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Elegant Background Watermark Number
            Text(
                text = "#${ticket.queueNumber}",
                fontSize = 100.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.04f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with subtle uppercase tracking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "DIGITAL TICKET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            color = TextGraySecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ticket.customerName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    }

                    StatusBadge(status = ticket.status, isPriority = ticket.isRejoinedPriority)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Queue Number & Ahead Metrics Grid (Elegant Dark 2-column layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Queue No. Card
                    Surface(
                        color = SalonDarkSubCardSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SalonDarkSubCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "QUEUE NO.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGraySecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format("#%02d", ticket.queueNumber),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isServing) StatusServingGreen else Color.White
                            )
                        }
                    }

                    // People Ahead Card
                    Surface(
                        color = SalonDarkSubCardSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SalonDarkSubCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "AHEAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGraySecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (isServing) "00" else String.format("%02d", ticketInfo.customersAhead),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isServing) StatusServingGreen else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isServing) "In Chair" else "People",
                                    fontSize = 11.sp,
                                    color = TextGraySecondary,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service Details & Est Turn
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SalonGoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ticket.service.iconEmoji,
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Text(
                                text = ticket.service.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "${ticket.service.durationMinutes} min service",
                                fontSize = 11.sp,
                                color = TextGraySecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isServing) "NOW" else ticketInfo.estimatedTurnTimeFormatted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SalonGoldPrimary
                        )
                        Text(
                            text = "EST. TURN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = TextGraySecondary
                        )
                    }
                }

                // Estimated Waiting Countdown Section
                if (isWaiting || isServing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SalonDarkCardBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isServing) "CURRENT SERVICE IN PROGRESS" else "ESTIMATED WAITING TIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = TextGraySecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isServing) {
                            Text(
                                text = "🎉 IN THE CHAIR NOW",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusServingGreen
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format("%02d", ticketInfo.estimatedWaitingMinutes),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White
                                )
                                Text(
                                    text = " min ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SalonGoldPrimary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (ticketInfo.estimatedWaitingSeconds > 0 || ticketInfo.estimatedWaitingMinutes == 0) {
                                    Text(
                                        text = String.format("%02d", ticketInfo.estimatedWaitingSeconds),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = SalonGoldLight
                                    )
                                    Text(
                                        text = " sec",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SalonGoldPrimary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Live queue sync active",
                                fontSize = 10.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = SalonGoldPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (isWaiting) {
                    Button(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x33450A0A)
                        ),
                        border = BorderStroke(1.dp, Color(0x667F1D1D)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("cancel_ticket_button")
                    ) {
                        Text(
                            text = "Cancel Ticket",
                            color = StatusCancelledRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else if (isCompleted || ticket.status == QueueStatus.CANCELLED || ticket.status == QueueStatus.SKIPPED) {
                    Button(
                        onClick = onClearTicket,
                        colors = ButtonDefaults.buttonColors(containerColor = SalonGoldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("get_new_ticket_button")
                    ) {
                        Text(text = "Get Another Queue Ticket", color = SalonDarkNavyBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = SalonDarkCardSurface,
            title = { Text(text = "Cancel Queue Ticket?", color = Color.White) },
            text = { Text("Are you sure you want to cancel Ticket #${ticket.queueNumber}? Your position will be released.", color = TextGraySecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelTicket(ticket.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCancelledRed)
                ) {
                    Text("Yes, Cancel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Ticket", color = TextGraySecondary)
                }
            }
        )
    }
}

@Composable
fun ServingChairCard(
    servingTicket: QueueTicket?,
    currentTime: Long,
    onCompleteService: (String) -> Unit,
    onSkipCustomer: (String) -> Unit,
    onStartNextCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (servingTicket != null) StatusServingGreen.copy(alpha = 0.08f) else SalonDarkCardSurface
        ),
        border = BorderStroke(
            1.dp,
            if (servingTicket != null) StatusServingGreen.copy(alpha = 0.4f) else SalonDarkCardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("serving_chair_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (servingTicket != null) StatusServingGreen else TextGrayMuted,
                                CircleShape
                            )
                    )
                    Text(
                        text = "CURRENT SERVING CHAIR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (servingTicket != null) StatusServingGreen else TextGraySecondary
                    )
                }

                if (servingTicket != null) {
                    StatusBadge(status = QueueStatus.SERVING)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (servingTicket != null) {
                val totalMinutes = servingTicket.service.durationMinutes
                val totalMillis = totalMinutes * 60_000L
                val elapsedMillis = if (servingTicket.startedAt != null) {
                    max(0L, currentTime - servingTicket.startedAt)
                } else 0L
                val remainingMillis = max(0L, totalMillis - elapsedMillis)

                val elapsedSecondsTotal = elapsedMillis / 1000L
                val elapsedMinutes = (elapsedSecondsTotal / 60L).toInt()
                val elapsedSeconds = (elapsedSecondsTotal % 60L).toInt()

                val remainingSecondsTotal = remainingMillis / 1000L
                val remainingMinutes = (remainingSecondsTotal / 60L).toInt()
                val remainingSeconds = (remainingSecondsTotal % 60L).toInt()

                val progress = if (totalMillis > 0) {
                    (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "#${servingTicket.queueNumber}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusServingGreen
                            )
                            Text(
                                text = servingTicket.customerName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "${servingTicket.service.iconEmoji} ${servingTicket.service.title} • ${servingTicket.service.durationMinutes} min",
                            fontSize = 12.sp,
                            color = SalonGoldLight,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar & Remaining Service Time
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Elapsed: ${elapsedMinutes}m ${String.format("%02d", elapsedSeconds)}s",
                            fontSize = 11.sp,
                            color = TextGraySecondary
                        )
                        Text(
                            text = "Remaining: ${remainingMinutes}m ${String.format("%02d", remainingSeconds)}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingMinutes <= 5 && remainingSecondsTotal > 0) StatusSkippedOrange else StatusServingGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StatusServingGreen,
                        trackColor = SalonDarkSubCardSurface,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons for current customer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSkipCustomer(servingTicket.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SalonDarkSubCardSurface),
                        border = BorderStroke(1.dp, SalonDarkSubCardBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("skip_current_customer_button")
                    ) {
                        Text("Skip", color = StatusSkippedOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onCompleteService(servingTicket.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusServingGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(44.dp)
                            .testTag("complete_service_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = SalonDarkNavyBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Complete Service", color = SalonDarkNavyBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                // Empty chair state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Barber chair is currently open",
                        fontSize = 13.sp,
                        color = TextGraySecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onStartNextCustomer,
                        colors = ButtonDefaults.buttonColors(containerColor = SalonGoldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("call_next_customer_button")
                    ) {
                        Text(text = "▶ Start Next Customer", color = SalonDarkNavyBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

