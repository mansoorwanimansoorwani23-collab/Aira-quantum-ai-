package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
fun ApiKeySetupDialog(
    initialGeminiKey: String,
    initialOpenAiKey: String,
    isValidating: Boolean,
    validationError: String?,
    onSaveAndContinue: (geminiKey: String, openAiKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var geminiKey by remember { mutableStateOf(initialGeminiKey) }
    var openAiKey by remember { mutableStateOf(initialOpenAiKey) }
    var showGeminiHelp by remember { mutableStateOf(false) }
    var showOpenAiHelp by remember { mutableStateOf(false) }

    var geminiKeyVisible by remember { mutableStateOf(false) }
    var openAiKeyVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("api_key_setup_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF040B07),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuantumOrbitalIcon(sizeDp = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "QUANTUM AI",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "DEVELOPED BY RAUF // AUTH REQUIRED",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Configure your AI neural credentials (Google Gemini, OpenAI, or both) to enable quantum intelligence.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )

                if (!validationError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3B0B11),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CrimsonRed)
                        )
                    ) {
                        Text(
                            text = "⚠ $validationError",
                            color = Color(0xFFFF8B9A),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Gemini API Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Gemini API Key",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { showGeminiHelp = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = "Gemini key instructions",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            placeholder = { Text("AIzaSy...", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (geminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                                    Icon(
                                        imageVector = if (geminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                openBrowserUrl(context, "https://aistudio.google.com/app/apikey")
                                showGeminiHelp = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("get_gemini_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = NeonGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Acquire Gemini Key (AI Studio)", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OpenAI API Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OpenAI API Key",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { showOpenAiHelp = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = "OpenAI key instructions",
                                    tint = SoftMagenta,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openAiKey,
                            onValueChange = { openAiKey = it },
                            placeholder = { Text("sk-...", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (openAiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { openAiKeyVisible = !openAiKeyVisible }) {
                                    Icon(
                                        imageVector = if (openAiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("openai_api_key_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                openBrowserUrl(context, "https://platform.openai.com/api-keys")
                                showOpenAiHelp = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("get_openai_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SoftMagenta
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Acquire OpenAI Key (Platform)", color = SoftMagenta, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_key_dialog_button")
                    ) {
                        Text("Later", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSaveAndContinue(geminiKey, openAiKey) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isValidating,
                        modifier = Modifier.testTag("save_and_continue_button")
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                color = Color(0xFF030705),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VALIDATING...", color = Color(0xFF030705), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        } else {
                            Text("AUTHENTICATE & START", color = Color(0xFF030705), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    // Help Dialog for Gemini
    if (showGeminiHelp) {
        AlertDialog(
            onDismissRequest = { showGeminiHelp = false },
            title = { Text("How to acquire Gemini API Key", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val steps = listOf(
                        "1. Sign in to your Google Account.",
                        "2. Navigate to Google AI Studio (aistudio.google.com).",
                        "3. Go to Get API key.",
                        "4. Create API key in project.",
                        "5. Copy key and paste into Aira."
                    )
                    steps.forEach { step ->
                        Text(text = step, color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openBrowserUrl(context, "https://aistudio.google.com/app/apikey")
                        showGeminiHelp = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Open AI Studio", color = Color(0xFF030705), fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGeminiHelp = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Help Dialog for OpenAI
    if (showOpenAiHelp) {
        AlertDialog(
            onDismissRequest = { showOpenAiHelp = false },
            title = { Text("How to acquire OpenAI API Key", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val steps = listOf(
                        "1. Sign in or create an OpenAI account.",
                        "2. Open the API Keys dashboard (platform.openai.com/api-keys).",
                        "3. Create new secret key.",
                        "4. Copy key and paste into Aira."
                    )
                    steps.forEach { step ->
                        Text(text = step, color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openBrowserUrl(context, "https://platform.openai.com/api-keys")
                        showOpenAiHelp = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftMagenta)
                ) {
                    Text("Open OpenAI Dashboard", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenAiHelp = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
