package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.model.DatingProfile
import com.example.model.SampleProfiles
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.DatingUiState
import com.example.viewmodel.DatingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase dynamically at startup
        com.example.service.FirebaseHelper.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry() {
    val datingViewModel: DatingViewModel = viewModel()
    val state by datingViewModel.uiState.collectAsState()

    val isOnboarded = state.isOnboarded
    var showSubscriptionSheet by remember { mutableStateOf(false) }

    // Navigation and BackPress Routing Layer
    BackHandler(enabled = true) {
        if (showSubscriptionSheet) {
            showSubscriptionSheet = false
        } else if (state.selectedProfileForDetail != null) {
            datingViewModel.selectProfileDetail(null)
        } else if (state.currentBottomTab != "discover") {
            datingViewModel.selectTab("discover")
        } else if (isOnboarded) {
            datingViewModel.setOnboarded(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Main Screen Router
        Crossfade(
            targetState = isOnboarded,
            animationSpec = tween(500),
            modifier = Modifier.fillMaxSize()
        ) { onboarded ->
            if (!onboarded) {
                // Left screen in design
                OnboardingScreen(
                    state = state,
                    onGetStarted = { name, phone, password ->
                        datingViewModel.registerUser(name, phone, password) { success ->
                            if (success) {
                                datingViewModel.setOnboarded(true)
                            }
                        }
                    },
                    onLogin = { phone, password, onResult ->
                        datingViewModel.loginUser(phone, password) { success ->
                            if (success) {
                                datingViewModel.setOnboarded(true)
                            }
                            onResult(success)
                        }
                    },
                    onClearError = {
                        datingViewModel.clearLoginError()
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Content Loader based on bottom selection
                    when (state.currentBottomTab) {
                        "discover" -> {
                            // Middle screen in design
                            DiscoverScreen(
                                state = state,
                                currentProfile = datingViewModel.currentProfile(),
                                onFilterSelected = { datingViewModel.setFilter(it) },
                                onTabSelected = { datingViewModel.selectTab(it) },
                                onLike = { datingViewModel.likeCurrentProfile() },
                                onPass = { datingViewModel.passCurrentProfile() },
                                onProfileSelected = { datingViewModel.selectProfileDetail(it) },
                                onReset = { datingViewModel.resetSwipes() },
                                onChatClicked = { datingViewModel.selectTab("chat") }
                            )
                        }
                        "matches" -> {
                            MatchesDashboardScreen(
                                state = state,
                                onProfileSelected = { datingViewModel.selectProfileDetail(it) },
                                onTabSelected = { datingViewModel.selectTab(it) }
                            )
                        }
                        "chat" -> {
                            ChatScreen(
                                state = state,
                                onSendMessage = { id, text -> datingViewModel.sendMessage(id, text) },
                                onActiveChatChanged = { id -> datingViewModel.setActiveChatRoom(id) }
                            )
                        }
                        "profile" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            PersonalProfileViewScreen(
                                state = state,
                                onSubscribeClick = { showSubscriptionSheet = true },
                                onTabSelected = { datingViewModel.selectTab(it) },
                                onTestFirebase = { datingViewModel.testFirebaseConnection() },
                                onSyncAllData = { datingViewModel.syncAllDataToFirebase() },
                                onImageSelected = { uri ->
                                    datingViewModel.uploadProfileImage(uri) { success, errorMsg ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Image de profil mise à jour !", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Échec du téléversement: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Floating Profile Detail Overlay
                    AnimatedVisibility(
                        visible = state.selectedProfileForDetail != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        state.selectedProfileForDetail?.let { detailProfile ->
                            ProfileDetailScreen(
                                profile = detailProfile,
                                isSubscribed = state.isSubscribed,
                                onUnlockClick = { showSubscriptionSheet = true },
                                onBack = { datingViewModel.selectProfileDetail(null) },
                                onLike = {
                                    datingViewModel.likeCurrentProfile()
                                    datingViewModel.selectProfileDetail(null)
                                },
                                onMessage = {
                                    datingViewModel.selectProfileDetail(null)
                                    datingViewModel.selectTab("chat")
                                }
                            )
                        }
                    }

                    // "ITS A MATCH !" Overlay Modal Dialog
                    AnimatedVisibility(
                        visible = state.matchShowcaseProfile != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.matchShowcaseProfile?.let { matchedProfile ->
                            MatchCelebrationModal(
                                matchedProfile = matchedProfile,
                                onSendMessage = {
                                    datingViewModel.dismissMatchModal()
                                    datingViewModel.selectTab("chat")
                                },
                                onDismiss = { datingViewModel.dismissMatchModal() }
                            )
                        }
                    }
                    
                    // Subscription Dialog Overlay
                    AnimatedVisibility(
                        visible = showSubscriptionSheet,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        SubscriptionDialog(
                            onDismiss = { showSubscriptionSheet = false },
                            onSubscribeSuccess = {
                                datingViewModel.setSubscriptionActive(true)
                                showSubscriptionSheet = false
                            }
                        )
                    }
                }
            }
        }

        // Gorgeous Fullscreen Loading Overlay covering the whole app during active operations
        AnimatedVisibility(
            visible = state.isFirebaseConnecting || state.isLoggingIn,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            PremiumLoadingScreen(
                message = when {
                    state.isLoggingIn -> "Recherche de votre compte et connexion sécurisée..."
                    state.userName.isNotBlank() && state.userName != "Joseph" -> "Création de votre profil et synchronisation..."
                    else -> "Préparation de l'univers de rencontre Firestore..."
                }
            )
        }
    }
}

// ------------------------------------------------------------------------
// SUB SCREEN: Matches Grid View (Tab 2)
// ------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesDashboardScreen(
    state: DatingUiState,
    onProfileSelected: (DatingProfile) -> Unit,
    onTabSelected: (String) -> Unit
) {
    val matchedList = state.profiles.filter {
        state.likedProfileIds.contains(it.id) || it.id == "1" || it.id == "2"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            CustomBottomNavigation(selectedTab = "matches", onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Text(
                text = "Vos Coups de Cœur",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            if (matchedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "No matches",
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucun favori pour l'instant",
                            color = GrayText,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(matchedList) { profile ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurface)
                                .clickable { onProfileSelected(profile) }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(profile.imageUrl),
                                contentDescription = profile.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "${profile.name}, ${profile.age}",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${profile.matchPercentage}% de compatibilité",
                                    color = BrandSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SUB SCREEN: Personal User Profile (Tab 4)
// ------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalProfileViewScreen(
    state: DatingUiState,
    onSubscribeClick: () -> Unit,
    onTabSelected: (String) -> Unit,
    onTestFirebase: () -> Unit,
    onSyncAllData: () -> Unit,
    onImageSelected: (android.net.Uri) -> Unit = {}
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            CustomBottomNavigation(selectedTab = "profile", onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mon Profil",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Start
            )

            // Large center avatar with neon rings and edit overlay
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(3.dp, if (state.isSubscribed) Color(0xFFFFD700) else BrandPrimary, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(state.userImageUrl),
                        contentDescription = "User Portrait",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Pencil edit overlay button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                        .border(2.dp, DarkBg, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.userName}, ${state.userAge}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.isSubscribed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFD700))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PREMIUM",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Text(
                text = "${state.userPhone} • Kinshasa, RDC",
                color = GrayText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Statistics Bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(count = "48k", label = "Vues")
                VerticalDivider(modifier = Modifier.width(1.dp).height(40.dp), color = Color.White.copy(alpha = 0.1f))
                ProfileStatItem(count = "2.4k", label = "Likes")
                VerticalDivider(modifier = Modifier.width(1.dp).height(40.dp), color = Color.White.copy(alpha = 0.1f))
                ProfileStatItem(count = "95", label = "Matchs")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BACKEND FIREBASE STATUS CARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Icon",
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Connexion Firebase",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Status badge
                    val isFbInited = com.example.service.FirebaseHelper.isInitialized
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFbInited) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isFbInited) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFbInited) "Activé" else "Mode Démo",
                            color = if (isFbInited) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(14.dp))

                // Credentials summary
                val projectId = com.example.BuildConfig.FIREBASE_PROJECT_ID
                val appId = com.example.BuildConfig.FIREBASE_APPLICATION_ID
                val isFbInitedSpec = com.example.service.FirebaseHelper.isInitialized

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Project ID :", color = GrayText, fontSize = 12.sp)
                        Text(
                            text = if (isFbInitedSpec && projectId.isNotBlank() && projectId != "YOUR_FIREBASE_PROJECT_ID_HERE") projectId else "Non configuré",
                            color = if (isFbInitedSpec) Color.White else GrayText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "App ID :", color = GrayText, fontSize = 12.sp)
                        Text(
                            text = if (isFbInitedSpec && appId.isNotBlank() && appId != "YOUR_FIREBASE_APP_ID_HERE" && appId.length > 25) {
                                "${appId.take(12)}...${appId.takeLast(10)}"
                            } else if (isFbInitedSpec && appId.isNotBlank() && appId != "YOUR_FIREBASE_APP_ID_HERE") {
                                appId
                            } else {
                                "N/A (Mode démo hors-ligne)"
                            },
                            color = if (isFbInitedSpec) Color.White else GrayText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action verification test button
                Button(
                    onClick = onTestFirebase,
                    enabled = !state.isTestingFirebase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("verify_firebase_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFbInitedSpec) BrandPrimary else Color.White.copy(alpha = 0.05f),
                        contentColor = if (isFbInitedSpec) Color.Black else Color.White.copy(alpha = 0.4f),
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isTestingFirebase) {
                        CircularProgressIndicator(
                            color = if (isFbInitedSpec) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Analyse en cours...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test Connection",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lancer le test de diagnostic (Écrit/Lu)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Backup/Seeder Data to Backend button
                Button(
                    onClick = onSyncAllData,
                    enabled = !state.isTestingFirebase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("backup_firestore_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFbInitedSpec) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f),
                        contentColor = if (isFbInitedSpec) Color.White else Color.White.copy(alpha = 0.4f),
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isTestingFirebase) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Sauvegarde en cours...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Backup to Firestore",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sauvegarder tout vers le Backend",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If testMessage isNotEmpty, show status result section
                if (state.firebaseTestMessage.isNotEmpty() || state.firebaseTestSuccess != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    val bannerBg = when (state.firebaseTestSuccess) {
                        true -> Color(0xFF10B981).copy(alpha = 0.08f)
                        false -> Color(0xFFEF4444).copy(alpha = 0.08f)
                        else -> Color.White.copy(alpha = 0.04f)
                    }
                    val bannerBorder = when (state.firebaseTestSuccess) {
                        true -> Color(0xFF10B981).copy(alpha = 0.15f)
                        false -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        else -> Color.White.copy(alpha = 0.08f)
                    }
                    val bannerIconColor = when (state.firebaseTestSuccess) {
                        true -> Color(0xFF10B981)
                        false -> Color(0xFFEF4444)
                        else -> Color.White.copy(alpha = 0.6f)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bannerBg)
                            .border(1.dp, bannerBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (state.firebaseTestSuccess) {
                                    true -> Icons.Default.CheckCircle
                                    false -> Icons.Default.Warning
                                    else -> Icons.Default.Info
                                },
                                contentDescription = "Test Status Icon",
                                tint = bannerIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Colorful status badge
                            val badgeLabel = when (state.firebaseTestSuccess) {
                                true -> "SUCCÈS"
                                false -> "ERREUR"
                                else -> "DIAGNOSTIC"
                            }
                            val badgeColor = when (state.firebaseTestSuccess) {
                                true -> Color(0xFF10B981)
                                false -> Color(0xFFEF4444)
                                else -> Color(0xFF3B82F6)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = state.firebaseTestMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Premium Banner Row
            SettingItemRow(
                icon = Icons.Default.Star,
                label = if (state.isSubscribed) "Gérer WeConnect Premium ⭐" else "Devenir Premium (Photo & Infos) ✨",
                onClick = onSubscribeClick
            )

            // Professional Premium Setting Rows
            SettingItemRow(icon = Icons.Default.Edit, label = "Modifier mon profil")
            SettingItemRow(icon = Icons.Default.Settings, label = "Paramètres de recherche")
            SettingItemRow(icon = Icons.Default.Lock, label = "Sécurité & Confidentialité")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = GrayText, fontSize = 12.sp)
    }
}

@Composable
fun SettingItemRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = BrandPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = "Arrow", tint = Color.White.copy(alpha = 0.35f))
    }
}

// ------------------------------------------------------------------------
// MODAL POPUP: WeConnect Premium Subscription Flow
// ------------------------------------------------------------------------
@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onSubscribeSuccess: () -> Unit
) {
    var selectedPlanIndex by remember { mutableStateOf(1) } // Default to Monthly (Popular)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        BoxWithConstraints {
            val maxHeightVal = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeightVal * 0.88f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF131118))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .clickable(enabled = false) { /* prevent backdrop close clicks */ }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
            // Accent drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium star",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WeConnect Premium",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Débloquez l'accès complet et illimité aux profils, bios, photos privées et discutez sans limites !",
                color = GrayText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Benefits Rows
            PremiumBenefitRow(text = "Débloquer toutes les silhouettes et photos de profil 🔓")
            PremiumBenefitRow(text = "Voir toutes les bios et styles de vie complets")
            PremiumBenefitRow(text = "Contacter directement toutes les célibataires")
            PremiumBenefitRow(text = "Zéro publicité & Priorité d'affichage ⚡")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Plans Selector Rows
            PlanOptionCard(
                planTitle = "Abonnement Hebdomadaire",
                planPrice = "2,99 €",
                planPeriod = "/ sem",
                isSelected = selectedPlanIndex == 0,
                isPopular = false,
                onClick = { selectedPlanIndex = 0 }
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            PlanOptionCard(
                planTitle = "Abonnement Mensuel",
                planPrice = "9,99 €",
                planPeriod = "/ mois",
                isSelected = selectedPlanIndex == 1,
                isPopular = true,
                onClick = { selectedPlanIndex = 1 }
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            PlanOptionCard(
                planTitle = "Abonnement Annuel",
                planPrice = "49,99 €",
                planPeriod = "/ an",
                isSelected = selectedPlanIndex == 2,
                isPopular = false,
                onClick = { selectedPlanIndex = 2 }
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // CTA Purchase Button
            Button(
                onClick = { onSubscribeSuccess() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("premium_purchase_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Devenir Premium ✨",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Sans engagement. Renouvellement automatique, résiliable à tout moment.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
      }
    }
}

@Composable
fun PremiumBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Inclus",
            tint = Color(0xFF00FF7F),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlanOptionCard(
    planTitle: String,
    planPrice: String,
    planPeriod: String,
    isSelected: Boolean,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) BrandPrimary.copy(alpha = 0.1f) else DarkSurface)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.1f))
                        .border(1.5.dp, if (isSelected) BrandPrimary else Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = planTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPopular) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "POPULAIRE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = planPrice,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = planPeriod,
                    color = GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------
// MODAL POPUP: It's a Match celebration
// ------------------------------------------------------------------------
@Composable
fun MatchCelebrationModal(
    matchedProfile: DatingProfile,
    onSendMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable(enabled = false) { /* prevent backdrop close clicks */ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "IT'S A MATCH !",
                color = BrandPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Vous et ${matchedProfile.name} vous plaisez mutuellement !",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Overlapping side-by-side matches avatar profiles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User avatar
                Image(
                    painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200"),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width((-18).dp)) // overlap look

                // Matched girl avatar
                Image(
                    painter = rememberAsyncImagePainter(matchedProfile.imageUrl),
                    contentDescription = "Matched Avatar",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(4.dp, BrandPrimary, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Action: Send first greeting text message
            Button(
                onClick = onSendMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("match_modal_msg_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Lui envoyer un message",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Dismiss overlay to keep swiping
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Continuer de chercher",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PremiumLoadingScreen(
    message: String = "Connexion et chargement de l'univers..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_anim")
    
    // Heart pulse scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    // Glowing halo ring rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotate"
    )

    // Pulse glows scale to intensify depth
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(enabled = false) { /* Prevent backdrop click intercepts */ }
            .testTag("premium_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background halo radial gradients
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer(scaleX = scale * 1.2f, scaleY = scale * 1.2f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandPrimary.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Heart Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                // Outlined rotating dotted circle
                Canvas(
                    modifier = Modifier
                        .size(110.dp)
                        .rotate(rotation)
                ) {
                    val strokeWidth = 3.dp.toPx()
                    val color = BrandPrimary.copy(alpha = 0.6f)
                    drawArc(
                        color = color,
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                // Central pulsing heart icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                        .shadow(16.dp, CircleShape, ambientColor = BrandPrimary, spotColor = BrandPrimary)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandPrimary, BrandSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Cœur de chargement",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Loading Text Label
            Text(
                text = "Chargement en cours...",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle contextual message
            Text(
                text = message,
                color = GrayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Subtle indicator
            CircularProgressIndicator(
                color = BrandPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
