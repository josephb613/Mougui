package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.DatingProfile
import com.example.ui.theme.*
import com.example.viewmodel.DatingUiState
import com.example.viewmodel.FilterType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DiscoverScreen(
    state: DatingUiState,
    currentProfile: DatingProfile?,
    onFilterSelected: (FilterType) -> Unit,
    onTabSelected: (String) -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onProfileSelected: (DatingProfile) -> Unit,
    onReset: () -> Unit,
    onChatClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Drag / Swipe offset trackers for manual gestures
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Reset offsets when profile shifts
    LaunchedEffect(currentProfile) {
        offsetX = 0f
        offsetY = 0f
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        containerColor = DarkBg,
        bottomBar = {
            // Capsule Bottom Navigation Bar exactly like screenshot
            CustomBottomNavigation(
                selectedTab = state.currentBottomTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Customized Top Bar matching design
            DiscoverTopHeader(
                currentFilter = state.currentFilter,
                onFilterSelected = onFilterSelected,
                notificationBadge = state.unreadNotificationsCount,
                userImageUrl = state.userImageUrl
            )

            // 2. Swiper Core Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentProfile != null) {
                    // Backing card stacked depth decorations (brown & green peeking edges in screenshot)
                    CardStackBackgrounds()

                    // Main interactive Swiping Card
                    val rotation = (offsetX / 60f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .rotate(rotation)
                            .shadow(24.dp, RoundedCornerShape(36.dp))
                            .clip(RoundedCornerShape(36.dp))
                            .background(DarkSurface)
                            // Swipe detection gesture controller!
                            .pointerInput(currentProfile.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (offsetX > 280) {
                                            coroutineScope.launch {
                                                onLike()
                                            }
                                        } else if (offsetX < -280) {
                                            coroutineScope.launch {
                                                onPass()
                                            }
                                        } else {
                                            // spring snap back
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y * 0.2f // dampen vertical movement
                                    }
                                )
                            }
                            .clickable { onProfileSelected(currentProfile) }
                            .testTag("interaction_swipe_profile_card")
                    ) {
                        // Profile portrait
                        Image(
                            painter = rememberAsyncImagePainter(currentProfile.imageUrl),
                            contentDescription = currentProfile.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Atmospheric twilight Vignette & Bottom Red/Pink Gradient glow matching Mari's card base
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.2f),
                                            BrandPrimary.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.95f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )

                        // Top swipe indicators (optional fun overlay when dragging)
                        if (offsetX != 0f) {
                            val alpha = (kotlin.math.abs(offsetX) / 150f).coerceIn(0f, 1f)
                            if (offsetX > 0) {
                                SwipeIndicatorLabel(text = "LIKE", color = Color.Green, modifier = Modifier.align(Alignment.TopStart).alpha(alpha))
                            } else {
                                SwipeIndicatorLabel(text = "NOPE", color = Color.Red, modifier = Modifier.align(Alignment.TopEnd).alpha(alpha))
                            }
                        }

                        // Bottom Profile text and Compatibility tag precisely styled
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 95% Compatibility Pill with Sparkle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Match percentage",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${currentProfile.matchPercentage}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Name & Age Heading matching Mari 26 typography
                            Text(
                                text = "${currentProfile.name} ${currentProfile.age}",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Distance location indicator with GPS Pin icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Pin Icon",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentProfile.distanceKm}km away",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Empty UX State with resetting trigger
                    DiscoverEmptyState(onReset)
                }
            }

            // 3. Round Action Floating Buttons (X, heart, chat) under the swipe card
            if (currentProfile != null) {
                DiscoverActionButtons(
                    onPass = onPass,
                    onLike = onLike,
                    onChat = onChatClicked
                )
            } else {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
fun DiscoverTopHeader(
    currentFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    notificationBadge: Int,
    userImageUrl: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User personal avatar in top left (dynamic profile photo from Firebase Storage!)
        Image(
            painter = rememberAsyncImagePainter(userImageUrl),
            contentDescription = "My Profile Avatar",
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .border(1.5.dp, BrandPrimary, CircleShape),
            contentScale = ContentScale.Crop
        )

        // Pill switches in Center: TODAY / FOR YOU
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODAY button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (currentFilter == FilterType.TODAY) TodayPinkBg else Color.Transparent)
                    .clickable { onFilterSelected(FilterType.TODAY) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TODAY",
                    color = if (currentFilter == FilterType.TODAY) Color.Black else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // FOR YOU button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (currentFilter == FilterType.FOR_YOU) TodayPinkBg else Color.Transparent)
                    .clickable { onFilterSelected(FilterType.FOR_YOU) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FOR YOU",
                    color = if (currentFilter == FilterType.FOR_YOU) Color.Black else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Notification Bell Icon with real-time badges
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { /* trigger notifications */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            if (notificationBadge > 0) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun CardStackBackgrounds() {
    Box(
        modifier = Modifier
            .fillMaxSize(0.95f)
            .padding(bottom = 12.dp)
    ) {
        // Olive Card edge top (third)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF8A8854)) // Dull Olive
        )
        // Brown Card edge top (second)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(48.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-7).dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF9E7C41)) // Soft ochre/brown
        )
    }
}

@Composable
fun DiscoverActionButtons(
    onPass: () -> Unit,
    onLike: () -> Unit,
    onChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dislike Cross (X) Button in dark background
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(onClick = onPass)
                .testTag("interaction_pass_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass profile",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Like Heart (♥️) central glowing pink-red button
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(16.dp, CircleShape, spotColor = BrandPrimary)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandSecondary, BrandPrimary)
                    )
                )
                .clickable(onClick = onLike)
                .testTag("interaction_like_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like profile",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Quick Message / Chat outline icon button
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(onClick = onChat)
                .testTag("interaction_chat_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = "Quick Chat",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DiscoverEmptyState(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TodayPinkBg.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "Empty profiles",
                tint = BrandPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Recherches terminées !",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Il n'y a plus aucun célibataire à proximité pour aujourd'hui. Elargissez votre périmètre !",
            color = GrayText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = "Recommencer la recherche", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SwipeIndicatorLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .border(4.dp, color, RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun CustomBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    // Beautiful, stylish rounded Capsule bottom bar exactly recreating the screenshot mockup
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xFF0F0F12))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(34.dp))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Discover tab (Selected looks like a cute pill in the mockup!)
            BottomNavItem(
                isSelected = selectedTab == "discover",
                icon = Icons.Default.Home,
                label = "Discover",
                onClick = { onTabSelected("discover") }
            )

            // 2. Hearts tab
            BottomNavItem(
                isSelected = selectedTab == "matches",
                icon = Icons.Default.Favorite,
                label = "",
                onClick = { onTabSelected("matches") }
            )

            // 3. Chat tab
            BottomNavItem(
                isSelected = selectedTab == "chat",
                icon = Icons.Default.ChatBubble,
                label = "",
                onClick = { onTabSelected("chat") }
            )

            // 4. Me/Profile tab
            BottomNavItem(
                isSelected = selectedTab == "profile",
                icon = Icons.Default.Person,
                label = "",
                onClick = { onTabSelected("profile") }
            )
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    if (isSelected && label.isNotEmpty()) {
        // Capsule pill pill highlighted layout matching selected Discover item
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        // Standard Icon layout
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label.ifEmpty { "Menu Item" },
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
