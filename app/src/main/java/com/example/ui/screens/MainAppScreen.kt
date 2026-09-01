package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ShopOpenBadge
import com.example.ui.screens.customer.CustomerScreen
import com.example.ui.screens.live.LiveBoardScreen
import com.example.ui.screens.owner.OwnerDashboardScreen
import com.example.ui.theme.SalonAmberAccent
import com.example.ui.theme.SalonDarkCardBorder
import com.example.ui.theme.SalonDarkCardSurface
import com.example.ui.theme.SalonDarkSubCardSurface
import com.example.ui.theme.SalonDarkNavyBackground
import com.example.ui.theme.SalonDarkNavySurface
import com.example.ui.theme.SalonGoldLight
import com.example.ui.theme.SalonGoldPrimary
import com.example.ui.theme.StatusServingGreen
import com.example.ui.theme.TextGrayMuted
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.SalonViewModel

@Composable
fun MainAppScreen(
    viewModel: SalonViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = SalonDarkNavyBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = SalonDarkNavySurface,
                border = BorderStroke(1.dp, SalonDarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SalonGoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Salon Logo",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STUDENT SALON",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = SalonGoldLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Telo, Chandrapura, Bokaro",
                            fontSize = 11.sp,
                            color = TextGraySecondary
                        )
                    }

                    ShopOpenBadge(isOpen = uiState.shopConfig.isOpen)
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SalonDarkNavySurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.CUSTOMER,
                    onClick = { viewModel.switchTab(AppTab.CUSTOMER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.CUSTOMER) Icons.Filled.ConfirmationNumber else Icons.Outlined.ConfirmationNumber,
                            contentDescription = "Customer Ticket"
                        )
                    },
                    label = { Text("My Ticket", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SalonGoldPrimary,
                        selectedTextColor = SalonGoldPrimary,
                        indicatorColor = SalonDarkSubCardSurface,
                        unselectedIconColor = TextGraySecondary,
                        unselectedTextColor = TextGraySecondary
                    ),
                    modifier = Modifier.testTag("nav_item_customer")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.LIVE_BOARD,
                    onClick = { viewModel.switchTab(AppTab.LIVE_BOARD) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.LIVE_BOARD) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Live Queue Board"
                        )
                    },
                    label = { Text("Live Queue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SalonGoldPrimary,
                        selectedTextColor = SalonGoldPrimary,
                        indicatorColor = SalonDarkSubCardSurface,
                        unselectedIconColor = TextGraySecondary,
                        unselectedTextColor = TextGraySecondary
                    ),
                    modifier = Modifier.testTag("nav_item_live_board")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.OWNER,
                    onClick = { viewModel.switchTab(AppTab.OWNER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.OWNER) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Owner Dashboard"
                        )
                    },
                    label = { Text("Owner", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SalonGoldPrimary,
                        selectedTextColor = SalonGoldPrimary,
                        indicatorColor = SalonDarkSubCardSurface,
                        unselectedIconColor = TextGraySecondary,
                        unselectedTextColor = TextGraySecondary
                    ),
                    modifier = Modifier.testTag("nav_item_owner")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.currentTab,
            label = "tabCrossfade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.CUSTOMER -> CustomerScreen(viewModel = viewModel, uiState = uiState)
                AppTab.LIVE_BOARD -> LiveBoardScreen(uiState = uiState)
                AppTab.OWNER -> OwnerDashboardScreen(viewModel = viewModel, uiState = uiState)
                AppTab.SALON_INFO -> CustomerScreen(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}
