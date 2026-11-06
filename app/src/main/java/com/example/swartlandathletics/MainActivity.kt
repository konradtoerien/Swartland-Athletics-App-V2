package com.example.swartlandathletics

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.swartlandathletics.auth.AuthManager
import com.example.swartlandathletics.auth.RoleManager
import com.example.swartlandathletics.auth.UserRole
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.ui.screens.*
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager
import com.example.swartlandathletics.util.WebDashboardConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Dashboard", Icons.Default.Home)
    object SetupImport : Screen("setup_import", "Atlete", Icons.Default.List)
    object DrawSheets : Screen("draw_sheets", "Lotings", Icons.Default.Info)
    object Officiating : Screen("officiating", "Beoordeel", Icons.Default.PlayArrow)
    object Records : Screen("records", "Rekords", Icons.Default.Star)
    object LiveFeed : Screen("live_feed", "Uitslae", Icons.Default.List)
    object Medals : Screen("medals", "Medaljes", Icons.Default.Star)
    object Reports : Screen("reports", "Verslae", Icons.Default.Info)
    object Program : Screen("program", "Program", Icons.Default.DateRange)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firestore centralized repository and role manager
        RoleManager.initialize(applicationContext)
        AthleticsRepository.initialize(applicationContext)

        setContent {
            SwartlandTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(currentUser == null) }
    var isGuestUser by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (currentUser != null) {
                isCheckingAuth = false
                RoleManager.setRole(UserRole.OFFICIAL)
                AthleticsRepository.startRealtimeSync()
            }
        }
        Firebase.auth.addAuthStateListener(listener)

        // Attempt silent background sign-in
        AuthManager.attemptAutoSignIn(
            context = context,
            onAuthSuccess = {
                isCheckingAuth = false
                currentUser = Firebase.auth.currentUser
                RoleManager.setRole(UserRole.OFFICIAL)
                AthleticsRepository.startRealtimeSync()
            },
            onUnauthenticated = {
                isCheckingAuth = false
            },
            scope = coroutineScope
        )

        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !isGuestUser) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                RoleManager.setRole(UserRole.OFFICIAL)
                AthleticsRepository.startRealtimeSync()
            },
            onContinueAsSpectator = {
                isGuestUser = true
                RoleManager.setRole(UserRole.SPECTATOR)
                AthleticsRepository.startRealtimeSync()
            },
            onOfficialPinEntered = {
                isGuestUser = true
                RoleManager.setRole(UserRole.OFFICIAL)
                AthleticsRepository.startRealtimeSync()
            }
        )
    } else {
        MainAppContent(
            currentUser = currentUser,
            onSignOut = {
                AuthManager.signOut(context, coroutineScope) {
                    currentUser = null
                    isGuestUser = false
                    RoleManager.lockToSpectator()
                }
            }
        )
    }
}

@Composable
fun SignInScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsSpectator: () -> Unit,
    onOfficialPinEntered: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val language by LanguageManager.currentLanguage.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LssNavyDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LssGold))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Language toggle in sign in screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LssNavyDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LssGold.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .testTag("signin_lang_toggle")
                            .clickable { LanguageManager.toggleLanguage() }
                    ) {
                        Text(
                            text = if (language == AppLanguage.AFRIKAANS) "🇿🇦 AFR" else "🇬🇧 ENG",
                            color = LssGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(LssNavyHeader)
                        .border(2.dp, LssGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lss_logo),
                        contentDescription = "Laerskool Swartland Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Laerskool Swartland",
                    color = LssGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (language == AppLanguage.ENGLISH) "Athletics Meet Portal" else "Atletiek Byeenkomsportaal",
                    color = LssTextLight,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (language == AppLanguage.ENGLISH)
                        "Officials sign in to record results. Parents and spectators can continue in view-only mode."
                    else
                        "Beamptes teken in om uitslae te beoordeel. Ouers en toeskouers kan dadelik in slegs-kyk modus voortgaan.",
                    color = LssTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = LssRedError.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = LssRedError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary 1: Official Google Sign-In
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val activity = context as? Activity
                        if (activity != null) {
                            AuthManager.onGoogleSignInClicked(
                                activity = activity,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                },
                                scope = coroutineScope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_google_sign_in"),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LssNavyHeader)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "Official Sign In (Google)" else "Beampte Teken In (Google)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary 2: View-Only Spectator / Parent Mode (Zero friction!)
                OutlinedButton(
                    onClick = onContinueAsSpectator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_continue_spectator"),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LssGold),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                ) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "👀 Continue as Spectator (View-Only)" else "👀 Gaan Voort as Toeskouer (Slegs-Kyk)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary 3: Official PIN access for officials without Google account
                TextButton(
                    onClick = {
                        enteredPin = ""
                        pinError = false
                        showPinDialog = true
                    },
                    modifier = Modifier.testTag("btn_pin_access")
                ) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "🔑 Official PIN Access (No Google Account)" else "🔑 Beampte PIN Toegang (Sonder Google)",
                        color = LssTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.ENGLISH) "🔒 Official PIN Access" else "🔒 Beampte PIN Toegang",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == AppLanguage.ENGLISH)
                            "Enter the 4-digit official access PIN (Default: 1920):"
                        else
                            "Voer die amptelike 4-syfer beampte PIN in (Verstek: 1920):",
                        color = LssTextLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 8) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LssGold,
                            unfocusedBorderColor = LssGold.copy(alpha = 0.5f),
                            focusedTextColor = LssTextLight,
                            unfocusedTextColor = LssTextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.rolePinIncorrect(language),
                            color = LssRedError,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (RoleManager.verifyPin(enteredPin)) {
                            showPinDialog = false
                            onOfficialPinEntered()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                ) {
                    Text(if (language == AppLanguage.ENGLISH) "Verify" else "Bevestig", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(L10n.cancel(language), color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun MainAppContent(
    currentUser: FirebaseUser?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val role by RoleManager.currentRole.collectAsState()

    var currentScreen by remember { mutableStateOf("home") }
    var officiatingEvent by remember { mutableStateOf<String?>(null) }
    var officiatingGender by remember { mutableStateOf<String?>(null) }
    var officiatingAge by remember { mutableStateOf<String?>(null) }

    // Dialog state for PIN verification when in spectator mode
    var showUnlockDialog by remember { mutableStateOf(false) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf(false) }

    // Navigation back stack handling
    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    // Role-based Navigation Items
    val bottomNavItems = remember(role) {
        if (role == UserRole.OFFICIAL) {
            listOf(
                Screen.Home,
                Screen.DrawSheets,
                Screen.Officiating,
                Screen.LiveFeed,
                Screen.Medals
            )
        } else {
            // Read-Only for Spectators
            listOf(
                Screen.Home,
                Screen.Program,
                Screen.LiveFeed,
                Screen.Medals,
                Screen.Records
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LssNavyDark),
        topBar = {
            Surface(
                color = LssNavyHeader,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Sync status and WhatsApp share button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LssGreenSuccess, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = L10n.synced(language),
                            color = LssTextLight,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // WhatsApp Share Action
                        IconButton(
                            onClick = { WebDashboardConfig.shareToWhatsApp(context, language) },
                            modifier = Modifier.size(28.dp).testTag("btn_topbar_share_whatsapp")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = L10n.shareWhatsAppLink(language),
                                tint = LssGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Right side: Language toggle, Role Chip, User status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick language switch in top bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LssNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LssGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .testTag("topbar_language_btn")
                                .clickable { LanguageManager.toggleLanguage() }
                        ) {
                            Text(
                                text = if (language == AppLanguage.AFRIKAANS) "🇿🇦 AFR" else "🇬🇧 ENG",
                                color = LssGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // ROLE BADGE (Clickable to switch or unlock)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (role == UserRole.OFFICIAL) LssGreenSuccess.copy(alpha = 0.2f) else LssGold.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (role == UserRole.OFFICIAL) LssGreenSuccess else LssGold),
                            modifier = Modifier
                                .testTag("topbar_role_badge")
                                .clickable {
                                    if (role == UserRole.SPECTATOR) {
                                        unlockPin = ""
                                        unlockError = false
                                        showUnlockDialog = true
                                    } else {
                                        RoleManager.lockToSpectator()
                                        Toast.makeText(context, if (language == AppLanguage.ENGLISH) "Locked to Spectator Mode" else "Gesluit na Toeskouer Modus", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Text(
                                text = if (role == UserRole.OFFICIAL) "⭐ Beampte" else "👀 Toeskouer",
                                color = if (role == UserRole.OFFICIAL) LssGreenSuccess else LssGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: (if (role == UserRole.OFFICIAL) "Beampte" else "Gaste"),
                            color = LssGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = L10n.signOut(language), tint = LssTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = LssNavyHeader,
                contentColor = LssGold,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentScreen == item.route
                    val itemTitle = when (item.route) {
                        "home" -> L10n.navHome(language)
                        "draw_sheets" -> L10n.navDraws(language)
                        "officiating" -> L10n.navOfficiating(language)
                        "live_feed" -> L10n.navResults(language)
                        "medals" -> L10n.navMedals(language)
                        "program" -> L10n.modProgramTitle(language).substringAfter(". ")
                        "records" -> L10n.modRecordsTitle(language).substringAfter(". ")
                        else -> item.title
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = item.route },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = itemTitle,
                                tint = if (isSelected) LssGold else LssTextMuted
                            )
                        },
                        label = {
                            Text(
                                text = itemTitle,
                                color = if (isSelected) LssGold else LssTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = LssNavyDark
                        ),
                        modifier = Modifier.testTag("nav_bar_${item.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    onNavigateTo = { route ->
                        if ((route == "setup_import" || route == "officiating") && role != UserRole.OFFICIAL) {
                            unlockPin = ""
                            unlockError = false
                            showUnlockDialog = true
                        } else {
                            currentScreen = route
                        }
                    }
                )
                "setup_import" -> {
                    if (role == UserRole.OFFICIAL) {
                        SetupImportScreen(onNavigateBack = { currentScreen = "home" })
                    } else {
                        currentScreen = "home"
                    }
                }
                "draw_sheets" -> DrawSheetsScreen(
                    onNavigateBack = { currentScreen = "home" },
                    onNavigateToOfficiating = { ev, g, a ->
                        officiatingEvent = ev
                        officiatingGender = g
                        officiatingAge = a
                        currentScreen = "officiating"
                    }
                )
                "officiating" -> {
                    if (role == UserRole.OFFICIAL) {
                        OfficiatingScreen(
                            initialEvent = officiatingEvent,
                            initialGender = officiatingGender,
                            initialAge = officiatingAge,
                            onNavigateBack = { currentScreen = "home" }
                        )
                    } else {
                        currentScreen = "home"
                    }
                }
                "records" -> RecordsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "live_feed" -> LiveFeedScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "medals" -> MedalsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "reports" -> ReportsScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
                "program" -> ProgramScreen(
                    onNavigateBack = { currentScreen = "home" }
                )
            }
        }
    }

    // Official PIN Unlock Dialog for Spectator -> Official promotion
    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.ENGLISH) "🔒 Unlock Official Mode" else "🔒 Ontsluit Beampte Modus",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == AppLanguage.ENGLISH)
                            "Enter the 4-digit official access PIN to record results and manage entries (Default: 1920):"
                        else
                            "Voer die amptelike 4-syfer beampte PIN in om uitslae aan te teken en inskrywings te bestuur (Verstek: 1920):",
                        color = LssTextLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = unlockPin,
                        onValueChange = {
                            if (it.length <= 8) {
                                unlockPin = it
                                unlockError = false
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = unlockError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LssGold,
                            unfocusedBorderColor = LssGold.copy(alpha = 0.5f),
                            focusedTextColor = LssTextLight,
                            unfocusedTextColor = LssTextLight
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (unlockError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L10n.rolePinIncorrect(language),
                            color = LssRedError,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (RoleManager.verifyPin(unlockPin)) {
                            showUnlockDialog = false
                            Toast.makeText(context, if (language == AppLanguage.ENGLISH) "Official Mode Unlocked!" else "Beampte Modus Ontsluit!", Toast.LENGTH_SHORT).show()
                        } else {
                            unlockError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                ) {
                    Text(if (language == AppLanguage.ENGLISH) "Unlock" else "Ontsluit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text(L10n.cancel(language), color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
