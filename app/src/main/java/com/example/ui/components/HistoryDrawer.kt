package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConversationEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onRenameConversation: (id: String, newTitle: String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameTargetCurrentTitle by remember { mutableStateOf("") }
    var renameInputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF040B07),
        modifier = Modifier.testTag("history_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                        text = "CONVERSATION ARCHIVES",
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // New Conversation Button
            Button(
                onClick = {
                    onNewChatClick()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("new_chat_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF030705),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INITIALIZE NEW SESSION",
                    color = Color(0xFF030705),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❯ NO PERSISTED LOGS FOUND",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        var menuExpanded by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) Color(0xFF0A2616) else DarkSurfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isActive) NeonGreen else DarkBorder
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectConversation(conv.id)
                                    onDismiss()
                                }
                                .testTag("conversation_item_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isActive) NeonGreen else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = conv.title,
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CyberBadge(
                                                text = if (conv.provider == "openai") "OPENAI" else "GEMINI",
                                                accentColor = if (conv.provider == "openai") SoftMagenta else NeonGreen
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Text(
                                                text = formatTimestamp(conv.updatedAt),
                                                color = TextMuted,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(DarkSurface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename", color = TextPrimary, fontFamily = FontFamily.Monospace) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonGreen) },
                                            onClick = {
                                                menuExpanded = false
                                                renameTargetId = conv.id
                                                renameTargetCurrentTitle = conv.title
                                                renameInputText = conv.title
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = CrimsonRed, fontFamily = FontFamily.Monospace) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonRed) },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteConversation(conv.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (onOpenSettings != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("drawer_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUANTUM SETTINGS & API KEYS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Rename Dialog
    if (renameTargetId != null) {
        AlertDialog(
            onDismissRequest = { renameTargetId = null },
            title = { Text("Rename Session Log", color = TextPrimary, fontFamily = FontFamily.Monospace) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = renameTargetId
                        if (targetId != null && renameInputText.isNotBlank()) {
                            onRenameConversation(targetId, renameInputText.trim())
                        }
                        renameTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Rename", color = Color(0xFF030705), fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetId = null }) {
                    Text("Cancel", color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
