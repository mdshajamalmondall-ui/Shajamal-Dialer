package com.example.ui.screens

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.data.model.DialerSettings
import com.example.data.model.DualSimMode
import com.example.data.model.PermissionSnapshot
import com.example.data.model.SimSlotInfo
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.components.DualSimCallBar
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme

private data class DialKey(
    val digit: Char,
    val letters: String
)

private val dialPadRows = listOf(
    listOf(DialKey('1', "∞"), DialKey('2', "ABC"), DialKey('3', "DEF")),
    listOf(DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO")),
    listOf(DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ")),
    listOf(DialKey('*', ""), DialKey('0', "+"), DialKey('#', ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    dialField: TextFieldValue,
    simSlots: List<SimSlotInfo>,
    exactMatchedContact: ContactItem?,
    numberNotes: Map<String, String>,
    isCurrentNumberBlocked: Boolean,
    settings: DialerSettings,
    permissions: PermissionSnapshot,
    isDefaultDialer: Boolean,
    onDialFieldChange: (TextFieldValue) -> Unit,
    onAppendChar: (Char) -> Unit,
    onLongPressZero: () -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCallSlot: (Int) -> Unit,
    onAskSimCall: () -> Unit,
    onSaveNote: (String, String) -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onRequestFullScreenIntent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pictureTheme = LocalPictureTheme.current
    val strings = LocalAppStrings.current
    val view = LocalView.current

    var showNoteDialog by remember { mutableStateOf(false) }

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { toneGenerator?.release() }
        }
    }

    fun playKeyFeedback(ch: Char) {
        if (settings.hapticFeedbackEnabled) {
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                HapticFeedbackConstants.KEYBOARD_TAP
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(flag)
        }
        if (settings.dtmfTonesEnabled && toneGenerator != null) {
            val tone = when (ch) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> -1
            }
            if (tone >= 0) {
                runCatching { toneGenerator.startTone(tone, 90) }
            }
        }
    }

    val currentNumber = dialField.text
    val canonicalKey = PhoneNumberInfoHelper.canonicalKey(currentNumber)
    val savedNote = if (canonicalKey.isNotBlank()) numberNotes[canonicalKey] else null
    val carrierInfo = remember(currentNumber) {
        PhoneNumberInfoHelper.detectCarrierAndRegion(currentNumber)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val isCompactHeight = maxHeight < 640.dp
        val maxKeyForWidth = ((maxWidth - 48.dp) / 3.3f).coerceIn(68.dp, 94.dp)
        val keyWidth = if (isCompactHeight) minOf(86.dp, maxKeyForWidth) else maxKeyForWidth
        val keyHeight = if (isCompactHeight) 52.dp else 58.dp
        val rowGap = if (isCompactHeight) 7.dp else 10.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Area: Brand, SIM carrier dots, Settings gear, and active caller/carrier card (no Suggested numbers list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimDotLabel(
                                    dotColor = pictureTheme.sim1Accent,
                                    text = simSlots.getOrNull(0)?.carrierName?.takeIf { it.isNotBlank() } ?: "SIM 1"
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                SimDotLabel(
                                    dotColor = Color(0xFF2563EB),
                                    text = simSlots.getOrNull(1)?.carrierName?.takeIf { it.isNotBlank() } ?: "SIM 2"
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isDefaultDialer) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onRequestDefaultDialer)
                                        .testTag("make_default_dialer_chip")
                                ) {
                                    Text(
                                        text = strings.setDefault,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            TopCornerSettingsButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("keypad_settings_button")
                            )
                        }
                    }

                    if (!permissions.hasCoreDialerPermissions || !permissions.postNotifications) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onRequestPermissions)
                                .testTag("grant_permissions_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.permBannerText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.allow,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (!permissions.canUseFullScreenIntent) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onRequestFullScreenIntent)
                                .testTag("grant_full_screen_calls_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.fullScreenCallsBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.allow,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Only show current caller/carrier info pill when a number is actively typed (no suggested list!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (currentNumber.isNotEmpty()) {
                        val cardColor = if (isCurrentNumberBlocked) {
                            CallMissedRed.copy(alpha = 0.14f)
                        } else {
                            Color(0xFFD9B872).copy(alpha = 0.22f)
                        }
                        val cardBorder = if (isCurrentNumberBlocked) {
                            CallMissedRed.copy(alpha = 0.6f)
                        } else {
                            Color(0xFF9C7A33).copy(alpha = 0.5f)
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardColor,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCurrentNumberBlocked) Icons.Rounded.Block else Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = if (isCurrentNumberBlocked) CallMissedRed else Color(0xFF926E22),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            isCurrentNumberBlocked -> strings.blockedNumberWarning
                                            exactMatchedContact != null -> exactMatchedContact.name
                                            else -> strings.unknownNumber
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrentNumberBlocked) CallMissedRed else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (!savedNote.isNullOrBlank()) {
                                            "$carrierInfo · $savedNote"
                                        } else {
                                            carrierInfo
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFD9B872).copy(alpha = 0.32f),
                                    border = BorderStroke(1.dp, Color(0xFF9C7A33).copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showNoteDialog = true }
                                        .testTag("dialer_tag_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = strings.tagLabel,
                                            tint = Color(0xFF8C691F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = strings.tagLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Keypad Sheet
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Number Input Box (Tap/Long-press gives native Paste/Copy/Cut/Autofill WITHOUT soft keyboard)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentNumber.isEmpty()) {
                                Text(
                                    text = strings.enterOrPasteHint,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                            val dynamicFontSize = when {
                                currentNumber.length > 15 -> 22.sp
                                currentNumber.length > 11 -> 26.sp
                                else -> 32.sp
                            }
                            @Suppress("DEPRECATION")
                            CompositionLocalProvider(LocalTextInputService provides null) {
                                BasicTextField(
                                    value = dialField,
                                    onValueChange = onDialFieldChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = dynamicFontSize,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialer_number_input")
                                )
                            }
                        }

                        if (currentNumber.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = onBackspace,
                                        onLongClick = onClearAll
                                    )
                                    .testTag("dialer_backspace_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = strings.holdToClear,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action Chips below number when number is entered: Save Contact, WhatsApp, Add Note, Block
                    AnimatedVisibility(visible = currentNumber.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            item {
                                DialerQuickActionChip(
                                    icon = Icons.Rounded.PersonAdd,
                                    label = strings.saveContact,
                                    iconTint = Color(0xFFB45309),
                                    onClick = {
                                        val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                            type = ContactsContract.RawContacts.CONTENT_TYPE
                                            putExtra(ContactsContract.Intents.Insert.PHONE, currentNumber)
                                        }
                                        runCatching { context.startActivity(insertIntent) }
                                    },
                                    modifier = Modifier.testTag("action_save_contact")
                                )
                            }
                            item {
                                DialerQuickActionChip(
                                    icon = Icons.Rounded.Chat,
                                    label = strings.whatsApp,
                                    iconTint = Color(0xFF16A34A),
                                    onClick = {
                                        val url = PhoneNumberInfoHelper.buildWhatsAppUrl(currentNumber)
                                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        runCatching { context.startActivity(waIntent) }
                                    },
                                    modifier = Modifier.testTag("action_whatsapp")
                                )
                            }
                            item {
                                DialerQuickActionChip(
                                    icon = Icons.Rounded.EditNote,
                                    label = strings.addNote,
                                    iconTint = Color(0xFF2563EB),
                                    onClick = { showNoteDialog = true },
                                    modifier = Modifier.testTag("action_add_note")
                                )
                            }
                            item {
                                DialerQuickActionChip(
                                    icon = Icons.Rounded.Block,
                                    label = if (isCurrentNumberBlocked) strings.unblock else strings.block,
                                    iconTint = CallMissedRed,
                                    onClick = { onToggleBlock(currentNumber, isCurrentNumberBlocked) },
                                    modifier = Modifier.testTag("action_block_number")
                                )
                            }
                        }
                    }

                    // Tactile Keypad Rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(rowGap),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        dialPadRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    DialPadKeyButton(
                                        dialKey = key,
                                        width = keyWidth,
                                        height = keyHeight,
                                        onTap = {
                                            playKeyFeedback(key.digit)
                                            onAppendChar(key.digit)
                                        },
                                        onLongPress = if (key.digit == '0') {
                                            {
                                                playKeyFeedback('0')
                                                onLongPressZero()
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // Call Button (opens SIM 1 / SIM 2 picker by default)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.dualSimMode == DualSimMode.DUAL_BUTTONS) {
                            DualSimCallBar(
                                simSlots = simSlots,
                                onCallSlot = onCallSlot
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(30.dp),
                                color = Color(0xFF16A34A),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .clickable(onClick = onAskSimCall)
                                    .testTag("ask_sim_call_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = strings.callButton,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = strings.callButton,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog && currentNumber.isNotEmpty()) {
        var noteText by remember(currentNumber, savedNote) { mutableStateOf(savedNote.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(strings.noteDialogTitle) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(strings.notePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(currentNumber, noteText)
                        showNoteDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun DialerQuickActionChip(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialPadKeyButton(
    dialKey: DialKey,
    width: Dp,
    height: Dp,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .testTag("dial_key_${dialKey.digit}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dialKey.digit.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )
            if (dialKey.letters.isNotEmpty()) {
                Text(
                    text = dialKey.letters,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SimDotLabel(
    dotColor: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
