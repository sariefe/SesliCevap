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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.domain.model.ChatMessage
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.VoiceInteractionScreen
import com.example.ui.screens.VoiceSettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VoiceAssistantViewModel
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import com.example.domain.model.ConversationSession
import com.example.domain.model.AnalyticsSummary
import com.example.ui.viewmodel.VoiceUiState
import dagger.hilt.android.AndroidEntryPoint

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
    ) { viewModel.playAudio(it, force = true) }
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
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(value = false) }
    var showVoiceInteractionScreen by remember { mutableStateOf(value = false) }

    var hasMicPermission by remember {
        mutableStateOf(
            value = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
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
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Sesli Asistan",
                        )
                    },
                    label = { Text("Sesli Asistan") },
                    modifier = Modifier.testTag("nav_assistant_tab"),
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Geçmiş",
                        )
                    },
                    label = { Text("Geçmiş") },
                    modifier = Modifier.testTag("nav_history_tab"),
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analiz",
                        )
                    },
                    label = { Text("Analiz") },
                    modifier = Modifier.testTag("nav_analytics_tab"),
                )
            }
        },
    ) { innerPadding ->
        when (currentTab) {
            0 -> {
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
                            if (uiState.speechState !is com.example.data.speech.SpeechState.Listening) {
                                onStartVoiceInput()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onDismissError = onDismissError,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            1 -> {
                HistoryScreen(
                    conversations = conversations,
                    onSelectConversation = { convId ->
                        onSelectConversation(convId)
                        currentTab = 0
                    },
                    onDeleteConversation = onDeleteConversation,
                    onClearAllHistory = onClearAllHistory,
                    onPlayAudio = onPlayAudioText,
                    getMessagesForConversation = getMessagesForConversation,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            2 -> {
                AnalyticsScreen(
                    analytics = analytics,
                    conversations = conversations,
                    onSelectConversation = { convId ->
                        onSelectConversation(convId)
                        currentTab = 0
                    },
                    onDeleteConversation = onDeleteConversation,
                    onClearAllHistory = onClearAllHistory,
                    modifier = Modifier.padding(innerPadding),
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
            onSelectPrompt = onSendTextMessage,
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
            analytics = AnalyticsSummary(),
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
        ) {}
    }
}
