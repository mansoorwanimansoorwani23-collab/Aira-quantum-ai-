package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIModelCatalog
import com.example.data.pref.PreferencesManager
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LaserLime
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.QuantumCyan
import com.example.ui.theme.SoftMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    selectedProvider: String,
    selectedGeminiModel: String,
    selectedOpenAiModel: String,
    geminiApiKey: String,
    openAiApiKey: String,
    autoVoiceSpeak: Boolean,
    isValidating: Boolean,
    validationFeedback: String?,
    onProviderChange: (String) -> Unit,
    onGeminiModelChange: (String) -> Unit,
    onOpenAiModelChange: (String) -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onOpenAiKeyChange: (String) -> Unit,
    onAutoVoiceSpeakChange: (Boolean) -> Unit,
    onValidateGeminiKey: (String) -> Unit,
    onValidateOpenAiKey: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editingGeminiKey by remember { mutableStateOf(geminiApiKey) }
    var editingOpenAiKey by remember { mutableStateOf(openAiApiKey) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF040B07),
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantumOrbitalIcon(sizeDp = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUANTUM SETTINGS",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Provider Selection Section
            Text(
                text = "❯ ACTIVE AI NEURAL ENGINE",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gemini Card
                ProviderOptionCard(
                    title = "Google Gemini",
                    subtitle = "DeepMind Engine",
                    isSelected = selectedProvider == PreferencesManager.PROVIDER_GEMINI,
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onProviderChange(PreferencesManager.PROVIDER_GEMINI) },
                    testTag = "select_gemini_provider_button"
                )

                // OpenAI Card
                ProviderOptionCard(
                    title = "OpenAI",
                    subtitle = "GPT-4o & o3",
                    isSelected = selectedProvider == PreferencesManager.PROVIDER_OPENAI,
                    accentColor = SoftMagenta,
                    modifier = Modifier.weight(1f),
                    onClick = { onProviderChange(PreferencesManager.PROVIDER_OPENAI) },
                    testTag = "select_openai_provider_button"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Model Selection Section
            Text(
                text = if (selectedProvider == PreferencesManager.PROVIDER_GEMINI) "❯ GEMINI MODELS CATALOG" else "❯ OPENAI MODELS CATALOG",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val currentModels = if (selectedProvider == PreferencesManager.PROVIDER_GEMINI) {
                AIModelCatalog.GEMINI_MODELS
            } else {
                AIModelCatalog.OPENAI_MODELS
            }

            val selectedModelId = if (selectedProvider == PreferencesManager.PROVIDER_GEMINI) {
                selectedGeminiModel
            } else {
                selectedOpenAiModel
            }

            currentModels.forEach { model ->
                val isSelected = model.id == selectedModelId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0C2417) else DarkSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSelected) NeonGreen else DarkBorder
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (selectedProvider == PreferencesManager.PROVIDER_GEMINI) {
                                onGeminiModelChange(model.id)
                            } else {
                                onOpenAiModelChange(model.id)
                            }
                        }
                        .testTag("model_option_${model.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (model.badge != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CyberBadge(text = model.badge, accentColor = NeonGreen)
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (selectedProvider == PreferencesManager.PROVIDER_GEMINI) {
                                        onGeminiModelChange(model.id)
                                    } else {
                                        onOpenAiModelChange(model.id)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeonGreen,
                                    unselectedColor = TextMuted
                                )
                            )
                        }

                        Text(
                            text = model.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            model.capabilities.forEach { cap ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF030A06)
                                ) {
                                    Text(
                                        text = cap,
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // API Keys Management
            Text(
                text = "❯ NEURAL API AUTHENTICATION",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Validation Feedback
            if (!validationFeedback.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF081C10),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = validationFeedback,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Gemini Key
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Google Gemini API Key", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingGeminiKey,
                        onValueChange = {
                            editingGeminiKey = it
                            onGeminiKeyChange(it)
                        },
                        placeholder = { Text("AIzaSy...", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_gemini_key_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onValidateGeminiKey(editingGeminiKey) },
                            enabled = !isValidating && editingGeminiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("validate_gemini_key_button")
                        ) {
                            Text("Validate Gemini Key", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // OpenAI Key
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "OpenAI API Key", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingOpenAiKey,
                        onValueChange = {
                            editingOpenAiKey = it
                            onOpenAiKeyChange(it)
                        },
                        placeholder = { Text("sk-...", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_openai_key_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onValidateOpenAiKey(editingOpenAiKey) },
                            enabled = !isValidating && editingOpenAiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("validate_openai_key_button")
                        ) {
                            Text("Validate OpenAI Key", color = SoftMagenta, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voice Settings
            Text(
                text = "❯ VOICE SYNTHESIS DIRECTIVES",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Transmit Audio Output",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Automatically speak responses during live voice session",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = autoVoiceSpeak,
                    onCheckedChange = onAutoVoiceSpeakChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF030705),
                        checkedTrackColor = NeonGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Danger Zone: Clear History
            Button(
                onClick = onClearAllHistory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B0B11)),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CrimsonRed)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_all_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("PURGE ALL CONVERSATION MEMORY", color = Color(0xFFFF8B9A), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Official Branding Footer
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF020704),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AIRA QUANTUM AI",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DEVELOPED BY RAUF",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quantum Core // Real-time Duplex Voice & Neural Execution",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ProviderOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF0A2215) else DarkSurfaceVariant,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) accentColor else DarkBorder
            )
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
