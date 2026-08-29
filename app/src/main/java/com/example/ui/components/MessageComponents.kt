package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import com.example.ui.theme.AssistantBubble
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlow
import com.example.ui.theme.LaserLime
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.QuantumCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToolBubble
import com.example.ui.theme.UserBubble

@Composable
fun MessageItemView(
    message: MessageEntity,
    isSpeaking: Boolean = false,
    onSpeakClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isUser = message.role == "user"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("message_item_${message.id}"),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.widthIn(max = 350.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    color = Color(0xFF041208),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.8f))
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        QuantumOrbitalIcon(sizeDp = 24.dp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // Tool Call Card if present
                if (!message.toolName.isNullOrBlank()) {
                    ToolExecutionCard(
                        toolName = message.toolName,
                        args = message.toolArgs,
                        result = message.toolResult
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Cyber Message Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    ),
                    color = if (isUser) UserBubble else AssistantBubble,
                    shadowElevation = 4.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isUser) LaserLime.copy(alpha = 0.5f) else DarkBorder
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Monospace role header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isUser) "❯ USER // COMMAND" else "❖ AIRA // QUANTUM CORE",
                                color = if (isUser) LaserLime else NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isUser) {
                            Text(
                                text = message.content,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 21.sp
                                )
                            )
                        } else {
                            RenderFormattedMarkdown(content = message.content)

                            // Actions row
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onSpeakClick != null) {
                                    IconButton(
                                        onClick = onSpeakClick,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                            contentDescription = "Read aloud",
                                            tint = if (isSpeaking) NeonGreen else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, message.content, "Message copied to clipboard")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    color = Color(0xFF0C2417),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(LaserLime.copy(alpha = 0.6f))
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "User",
                            tint = LaserLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolExecutionCard(
    toolName: String,
    args: String?,
    result: String?
) {
    val icon = when (toolName) {
        "openWhatsApp" -> Icons.Outlined.Language
        "openApp" -> Icons.Outlined.Smartphone
        "openUrl" -> Icons.Outlined.OpenInNew
        "makeCall" -> Icons.Default.Phone
        "callContact" -> Icons.Outlined.Call
        else -> Icons.Outlined.Settings
    }

    val displayTitle = when (toolName) {
        "openWhatsApp" -> "WHATSAPP_EXEC"
        "openApp" -> "APP_CONTROL_EXEC"
        "openUrl" -> "BROWSER_LINK_EXEC"
        "makeCall" -> "DIAL_PHONE_EXEC"
        "callContact" -> "CONTACT_CALL_EXEC"
        else -> "${toolName.uppercase()}_EXEC"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF030C07),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.6f))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = displayTitle,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ $displayTitle",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF092916),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(LaserLime)
                    )
                ) {
                    Text(
                        text = "ACCESS: GRANTED",
                        color = LaserLime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!result.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun RenderFormattedMarkdown(content: String) {
    val lines = content.split("\n")
    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = StringBuilder()

    Column {
        for (line in lines) {
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    CodeBlock(
                        code = codeContent.toString().trimEnd(),
                        language = codeLanguage
                    )
                    codeContent.clear()
                    inCodeBlock = false
                    codeLanguage = ""
                } else {
                    // Start code block
                    inCodeBlock = true
                    codeLanguage = line.trimStart().removePrefix("```").trim()
                }
                continue
            }

            if (inCodeBlock) {
                codeContent.append(line).append("\n")
                continue
            }

            // Normal text formatting
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        color = LaserLime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") || line.trimStart().startsWith("• ") -> {
                    val bulletText = line.trimStart().substring(2)
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "❖ ",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = parseBoldSpans(bulletText),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                line.isNotBlank() -> {
                    Text(
                        text = parseBoldSpans(line),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (inCodeBlock && codeContent.isNotEmpty()) {
            CodeBlock(code = codeContent.toString().trimEnd(), language = codeLanguage)
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF020704),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Code header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF06140A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Code",
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language.isNotBlank()) "[TERMINAL: ${language.uppercase()}]" else "[TERMINAL: CODE]",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        copyToClipboard(context, code, "Code snippet copied")
                        copied = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) LaserLime else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFD4FFE6),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private fun parseBoldSpans(text: String): androidx.compose.ui.text.AnnotatedString {
    val parts = text.split("**")
    return buildAnnotatedString {
        for (i in parts.indices) {
            if (i % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = NeonGreen)) {
                    append(parts[i])
                }
            } else {
                append(parts[i])
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String, toastMsg: String = "Copied to clipboard") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Aira AI", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}
