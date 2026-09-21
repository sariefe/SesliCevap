package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.speech.SpeechState
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.VoiceInteractionScreen
import com.example.ui.screens.VoiceSettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VoiceAssistantViewModel
import com.example.ui.viewmodel.VoiceUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

object ScreenRoutes {
    const val ASSISTANT = "assistant"
    const val HISTORY = "history"
    const val ANALYTICS = "analytics"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: VoiceAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                VoiceAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VoiceAppRoot(viewModel: VoiceAssistantViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val analytics by viewModel.analytics.collectAsState()

    VoiceAppRootContent(
        uiState = uiState,
        conversations = conversations,
        analytics = analytics,
        onStartVoiceInput = { viewModel.startVoiceInput() },
        onToggleVoiceListening = { viewModel.toggleVoiceListening() },
        onToggleVoiceMode = { viewModel.toggleVoiceEnabled() },
        onSendTextMessage = { viewModel.sendTextMessage(it) },
        onPlayAudio = { viewModel.playAudio(it.text) },
        onStopAudio = { viewModel.stopAudio() },
        onSelectConversation = { viewModel.selectConversation(it) },
        onNewConversation = { viewModel.createNewConversation() },
        onDeleteConversation = { viewModel.deleteConversation(it) },
        onClearAllHistory = { viewModel.clearAllHistory() },
        getMessagesForConversation = { viewModel.getMessagesForConversation(it) },
        onDismissError = { viewModel.dismissError() },
        onUpdateSettings = { isVoiceEnabled, autoSpeak, rate, pitch ->
            viewModel.updateSettings(isVoiceEnabled, autoSpeak, rate, pitch)
        },
        onPlayAudioText = { viewModel.playAudio(it, force = true) }
    )
}

@Composable
fun VoiceAppRootContent(
    uiState: VoiceUiState,
    conversations: List<ConversationSession>,
    analytics: AnalyticsSummary,
    onStartVoiceInput: () -> Unit,
    onToggleVoiceListening: () -> Unit,
    onToggleVoiceMode: () -> Unit,
    onSendTextMessage: (String) -> Unit,
    onPlayAudio: (ChatMessage) -> Unit,
    onStopAudio: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    getMessagesForConversation: (Long) -> Flow<List<ChatMessage>>,
    onDismissError: () -> Unit,
    onUpdateSettings: (Boolean, Boolean, Float, Float) -> Unit,
    onPlayAudioText: (String) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ScreenRoutes.ASSISTANT

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVoiceInteractionScreen by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            onStartVoiceInput()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentRoute == ScreenRoutes.ASSISTANT,
                    onClick = {
                        if (currentRoute != ScreenRoutes.ASSISTANT) {
                            navController.navigate(ScreenRoutes.ASSISTANT) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = stringResource(R.string.nav_assistant)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_assistant)) },
                    modifier = Modifier.testTag("nav_assistant_tab")
                )
                NavigationBarItem(
                    selected = currentRoute == ScreenRoutes.HISTORY,
                    onClick = {
                        if (currentRoute != ScreenRoutes.HISTORY) {
                            navController.navigate(ScreenRoutes.HISTORY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.nav_history)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_history)) },
                    modifier = Modifier.testTag("nav_history_tab")
                )
                NavigationBarItem(
                    selected = currentRoute == ScreenRoutes.ANALYTICS,
                    onClick = {
                        if (currentRoute != ScreenRoutes.ANALYTICS) {
                            navController.navigate(ScreenRoutes.ANALYTICS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = stringResource(R.string.nav_analytics)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_analytics)) },
                    modifier = Modifier.testTag("nav_analytics_tab")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoutes.ASSISTANT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ScreenRoutes.ASSISTANT) {
                VoiceAssistantScreen(
                    uiState = uiState,
                    conversations = conversations,
                    onMicClick = {
                        if (!uiState.isVoiceEnabled) {
                            showSettingsDialog = true
                        } else if (hasMicPermission) {
                            onToggleVoiceListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onToggleVoiceMode = onToggleVoiceMode,
                    onSendTextMessage = onSendTextMessage,
                    onPlayAudio = onPlayAudio,
                    onStopAudio = onStopAudio,
                    onSelectConversation = onSelectConversation,
                    onNewConversation = onNewConversation,
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenVoiceInteraction = {
                        if (!uiState.isVoiceEnabled) {
                            showSettingsDialog = true
                        } else if (hasMicPermission) {
                            showVoiceInteractionScreen = true
                            if (uiState.speechState !is SpeechState.Listening) {
                                onStartVoiceInput()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onDismissError = onDismissError
                )
            }
            composable(ScreenRoutes.HISTORY) {
                HistoryScreen(
                    conversations = conversations,
                    onSelectConversation = { convId ->
                        onSelectConversation(convId)
                        navController.navigate(ScreenRoutes.ASSISTANT) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onDeleteConversation = onDeleteConversation,
                    onClearAllHistory = onClearAllHistory,
                    onPlayAudio = onPlayAudioText,
                    getMessagesForConversation = getMessagesForConversation
                )
            }
            composable(ScreenRoutes.ANALYTICS) {
                AnalyticsScreen(
                    analytics = analytics,
                    conversations = conversations,
                    onSelectConversation = { convId ->
                        onSelectConversation(convId)
                        navController.navigate(ScreenRoutes.ASSISTANT) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onDeleteConversation = onDeleteConversation,
                    onClearAllHistory = onClearAllHistory
                )
            }
        }
    }

    if (showSettingsDialog) {
        VoiceSettingsDialog(
            initialIsVoiceEnabled = uiState.isVoiceEnabled,
            initialAutoSpeak = uiState.autoSpeak,
            initialSpeechRate = uiState.speechRate,
            initialSpeechPitch = uiState.speechPitch,
            onSave = { isVoiceEnabled, autoSpeak, rate, pitch ->
                onUpdateSettings(isVoiceEnabled, autoSpeak, rate, pitch)
            },
            onTestVoice = { _, _ ->
                onPlayAudioText("Merhaba! Sesli asistan test konuşması başarıyla yapılıyor.")
            }
        ) {
            showSettingsDialog = false
        }
    }

    if (showVoiceInteractionScreen) {
        VoiceInteractionScreen(
            uiState = uiState,
            onMicClick = {
                if (hasMicPermission) {
                    onToggleVoiceListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopAudio = onStopAudio,
            onClose = { showVoiceInteractionScreen = false },
            onOpenSettings = { showSettingsDialog = true },
            onSelectPrompt = onSendTextMessage
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceAppRootPreview() {
    MyApplicationTheme {
        VoiceAppRootContent(
            uiState = VoiceUiState(),
            conversations = emptyList(),
            analytics = AnalyticsSummary(
                totalConversations = 0,
                totalMessages = 0,
                totalUserQueries = 0,
                dominantSentiment = "—",
                dominantCategory = "—",
                mostUsedCategory = "",
                mostUsedSentiment = "",
                sentimentDistribution = emptyList(),
                categoryDistribution = emptyList()
            ),
            onStartVoiceInput = {},
            onToggleVoiceListening = {},
            onToggleVoiceMode = {},
            onSendTextMessage = {},
            onPlayAudio = {},
            onStopAudio = {},
            onSelectConversation = {},
            onNewConversation = {},
            onDeleteConversation = {},
            onClearAllHistory = {},
            getMessagesForConversation = { flowOf(emptyList()) },
            onDismissError = {},
            onUpdateSettings = { _, _, _, _ -> },
            onPlayAudioText = {}
        )
    }
}
