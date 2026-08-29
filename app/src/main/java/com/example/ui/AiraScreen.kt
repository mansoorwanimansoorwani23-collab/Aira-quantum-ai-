package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AIModelCatalog
import com.example.data.pref.PreferencesManager
import com.example.ui.components.ApiKeySetupDialog
import com.example.ui.components.CyberBadge
import com.example.ui.components.HistorySheet
import com.example.ui.components.LiveVoiceOverlay
import com.example.ui.components.MessageItemView
import com.example.ui.components.QuantumMatrixBackground
import com.example.ui.components.QuantumOrbitalIcon
import com.example.ui.components.SettingsSheet
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlow
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LaserLime
import com.example.ui.theme.MatrixEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.QuantumCyan
import com.example.ui.theme.SoftMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiraScreen(viewModel: AiraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val allConversations by viewModel.allConversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()

    val selectedProvider by viewModel.preferences.selectedProviderFlow.collectAsState()
    val selectedGeminiModel by viewModel.preferences.selectedGeminiModelFlow.collectAsState()
    val selectedOpenAiModel by viewModel.preferences.selectedOpenAiModelFlow.collectAsState()

    val voiceState by viewModel.liveVoiceManager.voiceState.collectAsState()
    val voiceStatusText by viewModel.liveVoiceManager.statusText.collectAsState()
    val audioAmplitude by viewModel.liveVoiceManager.audioAmplitude.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, uiState.isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    QuantumMatrixBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AiraTopBar(
                    selectedProvider = selectedProvider,
                    currentModelId = if (selectedProvider == PreferencesManager.PROVIDER_OPENAI) selectedOpenAiModel else selectedGeminiModel,
                    onModelSelect = { modelId ->
                        if (selectedProvider == PreferencesManager.PROVIDER_OPENAI) {
                            viewModel.preferences.setSelectedOpenAiModel(modelId)
                        } else {
                            viewModel.preferences.setSelectedGeminiModel(modelId)
                        }
                    },
                    onProviderSelect = { prov ->
                        viewModel.preferences.setSelectedProvider(prov)
                    },
                    onNewChatClick = { viewModel.createNewConversation() },
                    onHistoryClick = { viewModel.openHistory() },
                    onSettingsClick = { viewModel.openSettings() }
                )
            },
            bottomBar = {
                AiraInputBar(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    isLoading = uiState.isLoading,
                    onSendClick = {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(text)
                    },
                    onMicClick = {
                        viewModel.openLiveVoiceOverlay()
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty()) {
                    EmptyStateWelcome(
                        onPromptClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        },
                        onOpenVoice = { viewModel.openLiveVoiceOverlay() },
                        onOpenSettings = { viewModel.openSettings() }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageItemView(
                                message = msg,
                                isSpeaking = false,
                                onSpeakClick = null
                            )
                        }

                        if (uiState.isLoading) {
                            item {
                                LoadingAssistantIndicator(
                                    isExecutingTool = uiState.isExecutingTool,
                                    toolName = uiState.currentToolName
                                )
                            }
                        }
                    }
                }

                // Error Message banner
                if (uiState.errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3B0B11),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CrimsonRed)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠ " + (uiState.errorMessage ?: ""),
                                color = Color(0xFFFF8B9A),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.openSettings() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Voice Overlay
        AnimatedVisibility(
            visible = uiState.isLiveVoiceOverlayOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LiveVoiceOverlay(
                voiceState = voiceState,
                statusText = voiceStatusText,
                amplitude = audioAmplitude,
                onMicClick = { viewModel.toggleVoiceMic() },
                onInterruptClick = { viewModel.interruptVoice() },
                onCloseClick = { viewModel.closeLiveVoiceOverlay() }
            )
        }

        // Api Key Setup Dialog (First launch or manual)
        if (uiState.showApiKeyDialog) {
            ApiKeySetupDialog(
                initialGeminiKey = viewModel.preferences.getStoredGeminiApiKey(),
                initialOpenAiKey = viewModel.preferences.getOpenAiApiKey(),
                isValidating = uiState.isValidatingApiKey,
                validationError = uiState.validationFeedback,
                onSaveAndContinue = { geminiKey, openAiKey ->
                    viewModel.saveApiKeys(geminiKey, openAiKey)
                },
                onDismiss = { viewModel.closeApiKeyDialog() }
            )
        }

        // Settings Sheet
        if (uiState.showSettingsSheet) {
            SettingsSheet(
                selectedProvider = selectedProvider,
                selectedGeminiModel = selectedGeminiModel,
                selectedOpenAiModel = selectedOpenAiModel,
                geminiApiKey = viewModel.preferences.getStoredGeminiApiKey(),
                openAiApiKey = viewModel.preferences.getOpenAiApiKey(),
                autoVoiceSpeak = viewModel.preferences.isAutoVoiceSpeakEnabled(),
                isValidating = uiState.isValidatingApiKey,
                validationFeedback = uiState.validationFeedback,
                onProviderChange = { viewModel.preferences.setSelectedProvider(it) },
                onGeminiModelChange = { viewModel.preferences.setSelectedGeminiModel(it) },
                onOpenAiModelChange = { viewModel.preferences.setSelectedOpenAiModel(it) },
                onGeminiKeyChange = { viewModel.preferences.setGeminiApiKey(it) },
                onOpenAiKeyChange = { viewModel.preferences.setOpenAiApiKey(it) },
                onAutoVoiceSpeakChange = { viewModel.preferences.setAutoVoiceSpeakEnabled(it) },
                onValidateGeminiKey = { viewModel.validateGeminiKey(it) },
                onValidateOpenAiKey = { viewModel.validateOpenAiKey(it) },
                onClearAllHistory = { viewModel.clearAllHistory() },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // History Sheet
        if (uiState.showHistorySheet) {
            HistorySheet(
                conversations = allConversations,
                activeConversationId = activeConversationId,
                onSelectConversation = { viewModel.selectConversation(it) },
                onNewChatClick = { viewModel.createNewConversation() },
                onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                onDeleteConversation = { viewModel.deleteConversation(it) },
                onOpenSettings = { viewModel.openSettings() },
                onDismiss = { viewModel.closeHistory() }
            )
        }
    }
}

@Composable
fun AiraTopBar(
    selectedProvider: String,
    currentModelId: String,
    onModelSelect: (String) -> Unit,
    onProviderSelect: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val currentModel = AIModelCatalog.getModelById(currentModelId)

    Surface(
        color = Color(0xFF040A06),
        shadowElevation = 8.dp,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    bottom = 10.dp,
                    start = 14.dp,
                    end = 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantum Hacker Title & Developer Attribution
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantumOrbitalIcon(sizeDp = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AIRA QUANTUM AI",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CyberBadge(
                            text = if (selectedProvider == PreferencesManager.PROVIDER_OPENAI) "OPENAI" else "QUANTUM",
                            accentColor = if (selectedProvider == PreferencesManager.PROVIDER_OPENAI) SoftMagenta else NeonGreen
                        )
                    }
                    Text(
                        text = "DEVELOPED BY RAUF // SYSTEM: ONLINE",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right Action Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Model Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        ),
                        modifier = Modifier
                            .clickable { modelMenuExpanded = true }
                            .testTag("quick_model_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (currentModel?.name ?: currentModelId).replace("Gemini ", ""),
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        val availableModels = if (selectedProvider == PreferencesManager.PROVIDER_OPENAI) {
                            AIModelCatalog.OPENAI_MODELS
                        } else {
                            AIModelCatalog.GEMINI_MODELS
                        }
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.name.replace("Gemini ", ""), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(model.description, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                },
                                onClick = {
                                    onModelSelect(model.id)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("new_chat_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("history_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("settings_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = LaserLime,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiraInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomInset = maxOf(navPadding, imePadding)

    Surface(
        color = Color(0xFF040A06),
        shadowElevation = 10.dp,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = bottomInset + 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Voice Mode Button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonGreen, MatrixEmerald, LaserLime))
                    )
                    .border(1.5.dp, DarkBorderGlow, CircleShape)
                    .testTag("live_voice_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Start Live Voice Mode",
                    tint = Color(0xFF030705),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cyber Terminal Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = "❯ enter cyber command or prompt...",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) NeonGreen else DarkSurfaceVariant
                    )
                    .border(
                        1.dp,
                        if (inputText.isNotBlank() && !isLoading) LaserLime else DarkBorder,
                        CircleShape
                    )
                    .testTag("send_message_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color(0xFF030705) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStateWelcome(
    onPromptClick: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Quantum Hacker Hero Frame
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF05140A))
                .border(2.dp, NeonGreen, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_quantum_hacker),
                contentDescription = "Quantum Hacker",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title and Subtitle
        Text(
            text = "QUANTUM HACKER AI",
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "DEVELOPED BY RAUF // SYSTEM GRANTED",
            color = NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Matrix Binary Ticker Deco
        Text(
            text = "10100 010100 010000 01011 ❖ THINK QUANTUM • HACK EVERYTHING",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Cyber System Badges Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberBadge(text = "SYS: ACTIVE", accentColor = NeonGreen)
            CyberBadge(text = "QUANTUM CORE: 100%", accentColor = QuantumCyan)
            CyberBadge(text = "VOICE: LIVE DUPLEX", accentColor = LaserLime)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent Quick Actions: Live Voice & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Voice Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF092213),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenVoice() }
                    .testTag("home_open_voice_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Mode",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE VOICE",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Settings Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0A1822),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(QuantumCyan)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenSettings() }
                    .testTag("home_open_settings_button")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = QuantumCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SETTINGS",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Suggestion Chips
        Text(
            text = "❯ EXECUTE PRESET DIRECTIVES",
            color = NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        val prompts = listOf(
            "WhatsApp kholo",
            "Hindi mein baat karo",
            "Open YouTube",
            "Call Mummy",
            "Add emotions to script",
            "Voice analysis"
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { p ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF07180E),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clickable { onPromptClick(p) }
                        .testTag("suggestion_chip_${p.take(10)}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "❯ ",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = p,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingAssistantIndicator(
    isExecutingTool: Boolean,
    toolName: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            color = Color(0xFF081C10),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = if (isExecutingTool) LaserLime else NeonGreen,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = if (isExecutingTool) "⚡ EXECUTING NATIVE DIRECTIVE: ${toolName ?: "SYSTEM_TOOL"}..." else "❖ QUANTUM CORE PROCESSING...",
            color = if (isExecutingTool) LaserLime else NeonGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
