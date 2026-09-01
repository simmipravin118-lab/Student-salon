package com.example.ui.screens.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicket
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
import com.example.ui.theme.StatusRejoinedCyan
import com.example.ui.theme.StatusServingGreen
import com.example.ui.theme.StatusSkippedOrange
import com.example.ui.theme.StatusWaitingAmber
import com.example.ui.theme.TextGrayMuted
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.viewmodel.SalonUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun LiveBoardScreen(
    uiState: SalonUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SalonDarkNavyBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Board Header
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SalonGoldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Live Display",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "STUDENT SALON",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = SalonGoldLight,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Live Digital Queue Board",
                                    fontSize = 12.sp,
                                    color = TextGraySecondary
                                )
                            }
                        }

                        ShopOpenBadge(isOpen = uiState.shopConfig.isOpen)
                    }
                }
            }
        }

        // Live Serving Chair Hero
        item {
            val serving = uiState.servingTicket
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (serving != null) StatusServingGreen.copy(alpha = 0.12f) else SalonDarkCardSurface
                ),
                border = BorderStroke(
                    2.dp,
                    if (serving != null) StatusServingGreen else SalonDarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_board_serving_hero")
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (serving != null) StatusServingGreen else TextGrayMuted, CircleShape)
                            )
                            Text(
                                text = "NOW IN CHAIR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (serving != null) StatusServingGreen else TextGraySecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        if (serving != null) {
                            StatusBadge(status = QueueStatus.SERVING)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (serving != null) {
                        val totalMinutes = serving.service.durationMinutes
                        val totalMillis = totalMinutes * 60_000L
                        val elapsedMillis = if (serving.startedAt != null) {
                            max(0L, uiState.currentTimeMillis - serving.startedAt)
                        } else 0L
                        val remainingMillis = max(0L, totalMillis - elapsedMillis)

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
                                Text(
                                    text = "#${serving.queueNumber}",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StatusServingGreen
                                )
                                Text(
                                    text = serving.customerName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhitePrimary
                                )
                                Text(
                                    text = "${serving.service.iconEmoji} ${serving.service.title}",
                                    fontSize = 13.sp,
                                    color = SalonGoldLight
                                )
                            }

                            Surface(
                                color = SalonDarkSubCardSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, StatusServingGreen.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "REMAINING", fontSize = 10.sp, color = TextGrayMuted, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (remainingMinutes > 0)
                                            "${remainingMinutes}m ${String.format("%02d", remainingSeconds)}s"
                                        else
                                            "${remainingSeconds}s",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (remainingMinutes <= 5 && remainingSecondsTotal > 0) StatusSkippedOrange else StatusServingGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = StatusServingGreen,
                            trackColor = SalonDarkCardBorder
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Barber chair is ready for next customer",
                                fontSize = 14.sp,
                                color = TextGraySecondary
                            )
                        }
                    }
                }
            }
        }

        // Up Next Banner
        if (uiState.nextCustomerInLine != null) {
            item {
                val next = uiState.nextCustomerInLine
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                    border = BorderStroke(1.dp, StatusRejoinedCyan.copy(alpha = 0.4f)),
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
                                text = "UP NEXT (PREPARE)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusRejoinedCyan,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "#${next.queueNumber} - ${next.customerName}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhitePrimary
                            )
                            Text(
                                text = "${next.service.iconEmoji} ${next.service.title}",
                                fontSize = 12.sp,
                                color = TextGraySecondary
                            )
                        }

                        Surface(
                            color = StatusRejoinedCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Next In Line",
                                color = StatusRejoinedCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Waiting Queue List
        item {
            Text(
                text = "UPCOMING QUEUE LINEUP (${uiState.waitingTickets.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextGraySecondary,
                letterSpacing = 1.sp
            )
        }

        if (uiState.waitingTickets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SalonDarkCardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No one waiting right now. Walk in or join the queue to be served next!",
                        fontSize = 13.sp,
                        color = TextGraySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        } else {
            itemsIndexed(uiState.waitingTickets, key = { _, ticket -> ticket.id }) { index, ticket ->
                // Calculate estimated waiting for this index
                val serving = uiState.servingTicket
                val servingRemainingMillis = if (serving != null && serving.startedAt != null) {
                    val totalServingMillis = serving.service.durationMinutes * 60_000L
                    val elapsed = max(0L, uiState.currentTimeMillis - serving.startedAt)
                    max(0L, totalServingMillis - elapsed)
                } else if (serving != null) {
                    serving.service.durationMinutes * 60_000L
                } else 0L

                val waitPriorMillis = uiState.waitingTickets.take(index).sumOf { it.service.durationMinutes * 60_000L }
                val totalEstWaitMillis = servingRemainingMillis + waitPriorMillis
                val totalSeconds = totalEstWaitMillis / 1000L
                val estMinutes = (totalSeconds / 60L).toInt()
                val estSeconds = (totalSeconds % 60L).toInt()

                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val estTurnTime = if (totalEstWaitMillis == 0L) {
                    "${sdf.format(Date(uiState.currentTimeMillis))} (Now)"
                } else {
                    sdf.format(Date(uiState.currentTimeMillis + totalEstWaitMillis))
                }

                val waitFormatted = when {
                    estMinutes > 0 && estSeconds > 0 -> "Est: ${estMinutes}m ${estSeconds}s"
                    estMinutes > 0 -> "Est: ${estMinutes} min"
                    estSeconds > 0 -> "Est: ${estSeconds} sec"
                    else -> "Est: 0 min"
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ticket.isRejoinedPriority) StatusRejoinedCyan.copy(alpha = 0.08f) else SalonDarkCardSurface
                    ),
                    border = BorderStroke(1.dp, if (ticket.isRejoinedPriority) StatusRejoinedCyan else SalonDarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (ticket.isRejoinedPriority) StatusRejoinedCyan else SalonDarkSubCardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
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
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhitePrimary
                                    )
                                    if (ticket.isRejoinedPriority) {
                                        StatusBadge(status = QueueStatus.REJOINED)
                                    }
                                }

                                Text(
                                    text = "${ticket.service.iconEmoji} ${ticket.service.title}",
                                    fontSize = 12.sp,
                                    color = TextGraySecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = waitFormatted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SalonAmberAccent
                            )
                            Text(
                                text = estTurnTime,
                                fontSize = 11.sp,
                                color = StatusRejoinedCyan
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
