package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.system.VoiceCommandIntent
import com.example.ui.MeenaViewModel
import com.example.ui.components.CosmicBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.MeenaBottomBar
import com.example.ui.components.MeenaCharacterAvatar
import com.example.ui.components.MeenaDrawerContent
import com.example.ui.components.Routes
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageGenScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.QuickActionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.WriteStudioScreen
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MeenaAiProTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val viewModel: MeenaViewModel = viewModel()
            val appState by viewModel.state.collectAsStateWithLifecycle()
            val audioLevel by viewModel.voiceEngine.audioLevel.collectAsStateWithLifecycle()

            MeenaAiProTheme(themeAccent = appState.settings.themeAccent) {
                MeenaAppRoot(
                    viewModel = viewModel,
                    audioLevel = audioLevel
                )
            }
        }
    }
}

@Composable
private fun MeenaAppRoot(
    viewModel: MeenaViewModel,
    audioLevel: Float
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    CosmicBackground(reduceMotion = state.settings.reduceMotion) {
        if (!state.settings.isSignedIn) {
            CompanionWelcomeGate(
                state = state,
                onSignIn = { name, handle, email ->
                    viewModel.signInProfile(name, handle, email)
                }
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    MeenaDrawerContent(
                        currentRoute = currentRoute,
                        character = state.settings.selectedCharacter,
                        assistantName = state.settings.assistantName,
                        userName = state.settings.userName,
                        onNavigate = { route ->
                            scope.launch { drawerState.close() }
                            navigateTo(route)
                        }
                    )
                }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        MeenaBottomBar(
                            currentRoute = currentRoute,
                            character = state.settings.selectedCharacter,
                            onNavigate = { route -> navigateTo(route) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HOME,
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() }
                        ) {
                            composable(Routes.HOME) {
                                HomeScreen(
                                    state = state,
                                    audioLevel = audioLevel,
                                    hasAudioPermission = viewModel.phoneControl.hasRecordAudioPermission(),
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onNavigate = { navigateTo(it) },
                                    onStartVoice = { viewModel.startVoiceListening() },
                                    onStopVoice = { viewModel.stopVoiceListening() },
                                    onSendPrompt = { q ->
                                        navigateTo(Routes.CHAT)
                                        viewModel.sendChatMessage(q)
                                    }
                                )
                            }

                            composable(Routes.VOICE) {
                                VoiceAssistantScreen(
                                    state = state,
                                    audioLevel = audioLevel,
                                    hasAudioPermission = viewModel.phoneControl.hasRecordAudioPermission(),
                                    onBack = { navController.popBackStack() },
                                    onStartListen = { viewModel.startVoiceListening() },
                                    onStopListen = { viewModel.stopVoiceListening() },
                                    onInterrupt = { viewModel.interruptVoice() },
                                    onSubmitTextVoice = { viewModel.processVoiceTranscript(it) },
                                    onExecuteCommand = { cmd ->
                                        viewModel.clearPendingVoiceCommand()
                                        when (cmd) {
                                            is VoiceCommandIntent.OpenApp -> {
                                                viewModel.phoneControl.launchAppByName(cmd.appQuery)
                                                    .onFailure { err ->
                                                        scope.launch { snackbarHostState.showSnackbar(err.message ?: "App not found") }
                                                    }
                                            }
                                            is VoiceCommandIntent.Call -> {
                                                viewModel.phoneControl.placeCall(cmd.target)
                                            }
                                            is VoiceCommandIntent.OpenAlarm -> {
                                                viewModel.phoneControl.setAlarm(7, 0, "Meena Voice Alarm")
                                            }
                                            is VoiceCommandIntent.CheckWeather -> {
                                                navigateTo(Routes.QUICK_ACTIONS)
                                            }
                                            is VoiceCommandIntent.Flashlight -> {}
                                        }
                                    },
                                    onDismissCommand = { viewModel.clearPendingVoiceCommand() },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onTestVoice = { viewModel.testCurrentVoice() },
                                    onOpenConnections = { navigateTo(Routes.CONNECTIONS) }
                                )
                            }

                            composable(Routes.CHAT) {
                                ChatScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onSendMessage = { viewModel.sendChatMessage(it) },
                                    onRegenerate = { viewModel.regenerateLastResponse() },
                                    onNewConversation = { viewModel.startNewConversation() },
                                    onClearChat = { viewModel.clearCurrentChat() },
                                    onSearchQueryChange = { viewModel.setChatSearchQuery(it) },
                                    onAttachImageUri = { viewModel.setAttachedImageUri(it) },
                                    onToggleGroq = { viewModel.toggleGroqPreferred(it) },
                                    onSpeakAloud = { viewModel.speakTextAloud(it) },
                                    onSaveToMemory = { viewModel.addManualMemory(it, "Context") },
                                    onOpenVoice = { navigateTo(Routes.VOICE) },
                                    onOpenConnections = { navigateTo(Routes.CONNECTIONS) }
                                )
                            }

                            composable(Routes.HISTORY) {
                                HistoryScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onOpenConversation = { id ->
                                        viewModel.selectConversation(id)
                                        navigateTo(Routes.CHAT)
                                    },
                                    onNewChat = {
                                        viewModel.startNewConversation()
                                        navigateTo(Routes.CHAT)
                                    },
                                    onDeleteConversation = { viewModel.deleteConversation(it) },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }

                            composable(Routes.CHARACTERS) {
                                CharactersScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onSelectCharacter = { char ->
                                        viewModel.updateSettings { it.copy(selectedCharacter = char) }
                                        scope.launch { snackbarHostState.showSnackbar("${char.displayName} is now your active companion") }
                                    },
                                    onTestGreeting = { greeting -> viewModel.speakTextAloud(greeting) }
                                )
                            }

                            composable(Routes.CONNECTIONS) {
                                ConnectionsScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onSaveKey = { provider, key -> viewModel.saveApiKey(provider, key) },
                                    onClearKey = { provider -> viewModel.clearApiKey(provider) },
                                    onTestConnection = { provider -> viewModel.testConnection(provider) }
                                )
                            }

                            composable(Routes.QUICK_ACTIONS) {
                                QuickActionsScreen(
                                    state = state,
                                    phoneControl = viewModel.phoneControl,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navigateTo(it) },
                                    onToggleEnabled = { viewModel.toggleQuickActionEnabled(it) },
                                    onTogglePinned = { viewModel.toggleQuickActionPinned(it) },
                                    onMoveUp = { viewModel.moveQuickActionUp(it) },
                                    onFetchWeather = { viewModel.fetchWeatherForCity(it) },
                                    onAskMeenaTranslate = { prompt ->
                                        navigateTo(Routes.CHAT)
                                        viewModel.sendChatMessage(prompt)
                                    },
                                    onFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                )
                            }

                            composable(Routes.IMAGE_GEN) {
                                ImageGenScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onGenerate = { p, s, q, r -> viewModel.generateAiImage(p, s, q, r) },
                                    onDeleteImage = { viewModel.deleteGeneratedImage(it) },
                                    onSharePrompt = { viewModel.shareText(it) },
                                    onOpenConnections = { navigateTo(Routes.CONNECTIONS) }
                                )
                            }

                            composable(Routes.WRITE) {
                                WriteStudioScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onGenerate = { c, t, p -> viewModel.generateWrittenContent(c, t, p) },
                                    onShare = { viewModel.shareText(it) },
                                    onSaveMemory = { viewModel.addManualMemory(it, "Context") },
                                    onOpenConnections = { navigateTo(Routes.CONNECTIONS) }
                                )
                            }

                            composable(Routes.PHONE_CONTROL) {
                                PhoneControlScreen(
                                    state = state,
                                    phoneControl = viewModel.phoneControl,
                                    onBack = { navController.popBackStack() },
                                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                                    onVolumeChange = { viewModel.setMediaVolume(it) },
                                    onOpenSetting = { viewModel.openSystemSetting(it) },
                                    onRefreshApps = { viewModel.refreshInstalledApps() },
                                    onFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                )
                            }

                            composable(Routes.MEMORY) {
                                MemoryScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onToggleMemoryEnabled = { enabled ->
                                        viewModel.updateSettings { it.copy(memoryEnabled = enabled) }
                                    },
                                    onAddMemory = { fact, cat -> viewModel.addManualMemory(fact, cat) },
                                    onDeleteMemory = { viewModel.deleteMemory(it) },
                                    onClearAllMemories = { viewModel.clearAllMemories() },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }

                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onNavigate = { navigateTo(it) },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onTestVoice = { viewModel.testCurrentVoice() },
                                    onSignOut = { viewModel.signOut() },
                                    onDeleteAccount = { viewModel.deleteAccountAndWipeData() },
                                    onClearHistory = { viewModel.clearAllHistory() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionWelcomeGate(
    state: com.example.ui.state.MeenaAppState,
    onSignIn: (name: String, handle: String, email: String) -> Unit
) {
    val glow = LocalMeenaGlow.current
    var name by remember { mutableStateOf(state.settings.userName) }
    var handle by remember { mutableStateOf(state.settings.userHandle) }
    var email by remember { mutableStateOf(state.settings.accountEmail) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp)
    ) {
        GlassCard(modifier = Modifier.widthIn(max = 460.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeenaCharacterAvatar(
                    character = state.settings.selectedCharacter,
                    reduceMotion = state.settings.reduceMotion,
                    size = 132.dp
                )
                Text("MEENA AI PRO", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    "Your Personal AI Companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glow.secondaryGlow
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Local Profile)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onSignIn(name, handle, email) },
                    colors = ButtonDefaults.buttonColors(containerColor = glow.primaryGlow),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Enter MEENA AI PRO", color = TextPrimary)
                }
            }
        }
    }
}
