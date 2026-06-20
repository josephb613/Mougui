package com.example.ui.screens

import com.example.viewmodel.DatingUiState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GrayText

enum class OnboardingTab {
    WELCOME,
    REGISTRATION,
    LOGIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: DatingUiState,
    onGetStarted: (name: String, phone: String, password: String) -> Unit,
    onLogin: (phone: String, password: String, onResult: (Boolean) -> Unit) -> Unit,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(OnboardingTab.WELCOME) }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    var loginPhoneInput by remember { mutableStateOf("") }
    var loginPasswordInput by remember { mutableStateOf("") }
    var loginPhoneError by remember { mutableStateOf(false) }
    var loginPasswordError by remember { mutableStateOf(false) }
    var localLoginError by remember { mutableStateOf<String?>(null) }

    // Dynamic entry animations for a hyper-premium feel
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = twin(1000),
        label = "alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Couple Background Art with deep romantic overlay
        Image(
            painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1510154221590-ff63e90a136f?auto=format&fit=crop&q=80&w=800"),
            contentDescription = "Onboarding Couple Background",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.1f),
            contentScale = ContentScale.Crop
        )

        // Dark Atmospheric Radial & Linear Gradients to make the text jump
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // Animate between Welcome state and Phone Registration state
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "onboarding_step_transitions",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                OnboardingTab.WELCOME -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Elegant brand indicator at the top
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .alpha(alphaAnim)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FIND YOUR MATCH",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        // Typography and Button alignment
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                                .alpha(alphaAnim)
                                .scale(scaleAnim)
                        ) {
                            // Large styled title matching screenshot
                            Text(
                                text = "FIND\nYOUR\nLOVE",
                                color = Color.White,
                                fontSize = 58.sp,
                                lineHeight = 62.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            // High fidelity bottom pill customized launcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Get Started" Pill Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(BrandPrimary, BrandSecondary)
                                            )
                                        )
                                        .clickable { 
                                            currentTab = OnboardingTab.REGISTRATION 
                                            onClearError()
                                        }
                                        .testTag("onboarding_get_started"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Get Started",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Arrow Circle side decoration matching screenshot perfectly
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary)
                                        .clickable { 
                                            currentTab = OnboardingTab.REGISTRATION 
                                            onClearError()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward Button",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Aesthetic tiny terms caption
                            Text(
                                text = "En continuant, vous acceptez nos Conditions & Charte de Confidentialité.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                            )
                        }
                    }
                }

                OnboardingTab.REGISTRATION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header Back Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { 
                                        currentTab = OnboardingTab.WELCOME 
                                        onClearError()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "INSCRIPTION RAPIDE",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        // Registration Form block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Créer votre compte 🇨🇩",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Saisissez simplement vos informations pour vous connecter et commencer à discuter.",
                                color = GrayText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(36.dp))

                            // Name Field
                            Text(
                                text = "Votre Prénom",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    nameError = false
                                    onClearError()
                                },
                                placeholder = { Text("ex: Joseph", color = Color.White.copy(alpha = 0.35f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_name_input"),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Nom",
                                        tint = BrandPrimary
                                    )
                                },
                                singleLine = true,
                                isError = nameError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            if (nameError) {
                                Text(
                                    text = "Veuillez entrer votre prénom",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // RDC (+243) Phone Field
                            Text(
                                text = "Numéro de téléphone (RDC)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { newValue ->
                                    // Strip non-numerical to protect layout and validation
                                    var cleaned = newValue.filter { it.isDigit() }
                                    if (cleaned.startsWith("0")) {
                                        cleaned = cleaned.drop(1)
                                    }
                                    if (cleaned.length <= 9) {
                                        phoneInput = cleaned
                                        phoneError = false
                                    }
                                    onClearError()
                                },
                                placeholder = { Text("812 345 678", color = Color.White.copy(alpha = 0.35f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_phone_input"),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp)
                                    ) {
                                        Text(text = "🇨🇩", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+243",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = phoneError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            if (phoneError) {
                                Text(
                                    text = "Veuillez entrer un numéro de téléphone RDC valide (9 chiffres)",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "Entrez vos 9 chiffres (ex: 812 345 678)",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Password Field
                            Text(
                                text = "Créer votre Mot de passe",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = false
                                    onClearError()
                                },
                                placeholder = { Text("6 caractères minimum", color = Color.White.copy(alpha = 0.35f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_password_input"),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Mot de passe",
                                        tint = BrandPrimary
                                    )
                                },
                                singleLine = true,
                                isError = passwordError,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            if (state.loginError != null) {
                                Text(
                                    text = state.loginError,
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (passwordError) {
                                Text(
                                    text = "Le mot de passe doit contenir au moins 6 caractères",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }

                        // Bottom Action Button & Switch Link
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    val isNameVal = nameInput.isNotBlank()
                                    // DRC phone numbers typically have 9 digits (e.g. 812345678)
                                    val isPhoneVal = phoneInput.length == 9
                                    val isPasswordVal = passwordInput.length >= 6

                                    nameError = !isNameVal
                                    phoneError = !isPhoneVal
                                    passwordError = !isPasswordVal

                                    if (isNameVal && isPhoneVal && isPasswordVal) {
                                        // Formatter: e.g. "812345678" -> "+243 812 345 678"
                                        val part1 = phoneInput.substring(0, 3)
                                        val part2 = phoneInput.substring(3, 6)
                                        val part3 = phoneInput.substring(6, 9)
                                        val formatted = "+243 $part1 $part2 $part3"
                                        onGetStarted(nameInput.trim(), formatted, passwordInput)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("onboarding_submit_registration"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Register",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Créer mon Compte ✨",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Déjà inscrit(e) ? ",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Connectez-vous !",
                                    color = BrandPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { 
                                            currentTab = OnboardingTab.LOGIN 
                                            localLoginError = null
                                            onClearError()
                                        }
                                        .testTag("onboarding_toggle_to_login")
                                )
                            }
                        }
                    }
                }

                OnboardingTab.LOGIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header Back Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { 
                                        currentTab = OnboardingTab.WELCOME 
                                        onClearError()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "CONNEXION",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        // Login Form block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Bon retour ! 😊",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Saisissez votre numéro de téléphone pour retrouver vos matchs et discussions.",
                                color = GrayText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(36.dp))

                            // RDC (+243) Phone Field
                            Text(
                                text = "Votre Numéro de téléphone (RDC)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = loginPhoneInput,
                                onValueChange = { newValue ->
                                    var cleaned = newValue.filter { it.isDigit() }
                                    if (cleaned.startsWith("0")) {
                                        cleaned = cleaned.drop(1)
                                    }
                                    if (cleaned.length <= 9) {
                                        loginPhoneInput = cleaned
                                        loginPhoneError = false
                                    }
                                    localLoginError = null
                                    onClearError()
                                },
                                placeholder = { Text("812 345 678", color = Color.White.copy(alpha = 0.35f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_login_phone_input"),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp)
                                    ) {
                                        Text(text = "🇨🇩", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+243",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = loginPhoneError || localLoginError != null || state.loginError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            
                            val displayError = localLoginError ?: state.loginError
                            if (loginPhoneError) {
                                Text(
                                    text = "Veuillez entrer un numéro de téléphone RDC valide (9 chiffres)",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            } else if (displayError != null) {
                                Text(
                                    text = displayError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "Entrez vos 9 chiffres (ex: 812 345 678)",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Password Field
                            Text(
                                text = "Votre Mot de passe",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = loginPasswordInput,
                                onValueChange = {
                                    loginPasswordInput = it
                                    loginPasswordError = false
                                    onClearError()
                                },
                                placeholder = { Text("Entrez votre mot de passe", color = Color.White.copy(alpha = 0.35f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_login_password_input"),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Mot de passe",
                                        tint = BrandPrimary
                                    )
                                },
                                singleLine = true,
                                isError = loginPasswordError,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            if (loginPasswordError) {
                                Text(
                                    text = "Veuillez entrer votre mot de passe (6 caractères min)",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Link to Registration Screen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Pas encore inscrit(e) ? ",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Créez votre compte !",
                                    color = BrandPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { 
                                            currentTab = OnboardingTab.REGISTRATION 
                                            localLoginError = null
                                            onClearError()
                                        }
                                        .testTag("onboarding_toggle_to_register")
                                )
                            }
                        }

                        // Bottom Action Button
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val isPhoneVal = loginPhoneInput.length == 9
                                    val isPasswordVal = loginPasswordInput.length >= 6
                                    loginPhoneError = !isPhoneVal
                                    loginPasswordError = !isPasswordVal

                                    if (isPhoneVal && isPasswordVal) {
                                        val part1 = loginPhoneInput.substring(0, 3)
                                        val part2 = loginPhoneInput.substring(3, 6)
                                        val part3 = loginPhoneInput.substring(6, 9)
                                        val formatted = "+243 $part1 $part2 $part3"
                                        
                                        onLogin(formatted, loginPasswordInput) { success ->
                                            if (!success) {
                                                // Fail triggers state.loginError display dynamically
                                            }
                                        }
                                    }
                                },
                                enabled = !state.isLoggingIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("onboarding_submit_login"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                if (state.isLoggingIn) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Connexion...",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Login",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Se connecter ✨",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility extension for quick animation specification
fun <T> twin(duration: Int): TweenSpec<T> = tween(durationMillis = duration, easing = FastOutSlowInEasing)
