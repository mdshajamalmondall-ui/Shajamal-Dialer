package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CallerProfileTarget
import com.example.data.model.FavoriteGroup
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactProfileScreen(
    target: CallerProfileTarget,
    savedNote: String?,
    selectedGroup: FavoriteGroup,
    isBlocked: Boolean,
    language: AppLanguage,
    onBack: () -> Unit,
    onToggleStar: () -> Unit,
    onCallSim1: () -> Unit,
    onCallSim2: () -> Unit,
    onSelectGroup: (FavoriteGroup) -> Unit,
    onSaveNote: (String) -> Unit,
    onToggleBlock: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val clipboardManager = LocalClipboardManager.current

    val formattedNumber = remember(target.number) {
        PhoneNumberInfoHelper.formatDisplayNumber(target.number).ifBlank { target.number }
    }
    val carrierAndRegion = remember(target.number) {
        PhoneNumberInfoHelper.detectCarrierAndRegion(target.number)
    }

    var noteText by remember(target.number, savedNote) {
        mutableStateOf(savedNote.orEmpty())
    }

    val badgeLabel = when (selectedGroup) {
        FavoriteGroup.NONE -> strings.personalBadge
        else -> if (language == AppLanguage.BENGALI) selectedGroup.labelBn else selectedGroup.labelEn
    }

    fun openContactEditor() {
        if (target.contactId != null) {
            val contactUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                target.contactId.toString()
            )
            val editIntent = Intent(Intent.ACTION_EDIT, contactUri)
            runCatching { context.startActivity(editIntent) }
                .onFailure {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, contactUri)) }
                }
        } else {
            val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, target.number)
                if (target.displayTitle != formattedNumber) {
                    putExtra(ContactsContract.Intents.Insert.NAME, target.displayTitle)
                }
            }
            runCatching { context.startActivity(insertIntent) }
        }
    }

    fun scheduleReminder(offsetMillis: Long?, tomorrowNineAm: Boolean, label: String) {
        val startMillis = if (tomorrowNineAm) {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            System.currentTimeMillis() + (offsetMillis ?: (15L * 60L * 1000L))
        }
        val calIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Call ${target.displayTitle}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Shajamal reminder ($label) for ${target.number}"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 15L * 60L * 1000L)
        }
        runCatching { context.startActivity(calIntent) }
            .onSuccess { onShowMessage("Reminder: $label") }
            .onFailure { onShowMessage("Could not open Calendar app") }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = strings.contactProfile,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.testTag("profile_star_button")
            ) {
                Icon(
                    imageVector = if (target.isStarred) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (target.isStarred) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = ::openContactEditor,
                modifier = Modifier.testTag("profile_edit_contact_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = strings.edit,
                    tint = Color(0xFF15803D)
                )
            }
        }

        // Card 1: Hero Card (Photo, Name, Number, Carrier, Group Badge, 5 Quick Actions)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    modifier = Modifier.size(92.dp)
                ) {
                    if (!target.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = target.photoUri,
                            contentDescription = target.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = target.displayTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = target.displayTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formattedNumber,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF15803D)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = carrierAndRegion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF2563EB).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 5 Quick Action Circles: SIM 1, SIM 2, WA, SMS, Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    ProfileQuickCircleAction(
                        icon = Icons.Rounded.Call,
                        label = "SIM 1",
                        containerColor = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF15803D),
                        onClick = onCallSim1,
                        modifier = Modifier.testTag("profile_sim1_button")
                    )
                    ProfileQuickCircleAction(
                        icon = Icons.Rounded.SimCard,
                        label = "SIM 2",
                        containerColor = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF1D4ED8),
                        onClick = onCallSim2,
                        modifier = Modifier.testTag("profile_sim2_button")
                    )
                    ProfileQuickCircleAction(
                        icon = Icons.Rounded.Chat,
                        label = strings.waLabel,
                        containerColor = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A),
                        onClick = {
                            val url = PhoneNumberInfoHelper.buildWhatsAppUrl(target.number)
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }.onFailure {
                                onShowMessage("WhatsApp is not installed")
                            }
                        },
                        modifier = Modifier.testTag("profile_whatsapp_button")
                    )
                    ProfileQuickCircleAction(
                        icon = Icons.Rounded.Sms,
                        label = strings.smsLabel,
                        containerColor = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFB45309),
                        onClick = {
                            val smsIntent = Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("smsto:${Uri.encode(target.number)}")
                            )
                            runCatching { context.startActivity(smsIntent) }
                                .onFailure { onShowMessage("No SMS app found") }
                        },
                        modifier = Modifier.testTag("profile_sms_button")
                    )
                    ProfileQuickCircleAction(
                        icon = Icons.Rounded.ContentCopy,
                        label = strings.copy,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        isRoundedRect = true,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(target.number))
                            onShowMessage("Copied ${target.number}")
                        },
                        modifier = Modifier.testTag("profile_copy_button")
                    )
                }
            }
        }

        // Card 2: Favorite Group
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = Color(0xFF9A7516),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.favoriteGroupTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FavoriteGroup.entries.forEach { group ->
                        val isSelected = group == selectedGroup
                        val label = if (language == AppLanguage.BENGALI) group.labelBn else group.labelEn
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                Color(0xFFFDF0CF)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    Color(0xFFE0BA63)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectGroup(group) }
                                .testTag("profile_group_${group.id}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF926E22) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        // Card 3: Number Note & Call Reminder
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.numberNoteAndReminderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(strings.notePlaceholder) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_note_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onSaveNote(noteText) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9A7516),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("profile_save_note_button")
                    ) {
                        Text(
                            text = strings.saveNoteButton,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = Color(0xFF9A7516),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.scheduleCallReminder,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReminderPresetChip(
                        label = strings.remind15m,
                        onClick = { scheduleReminder(15L * 60L * 1000L, false, strings.remind15m) }
                    )
                    ReminderPresetChip(
                        label = strings.remind30m,
                        onClick = { scheduleReminder(30L * 60L * 1000L, false, strings.remind30m) }
                    )
                    ReminderPresetChip(
                        label = strings.remind1h,
                        onClick = { scheduleReminder(60L * 60L * 1000L, false, strings.remind1h) }
                    )
                    ReminderPresetChip(
                        label = strings.remindTomorrow9am,
                        onClick = { scheduleReminder(null, true, strings.remindTomorrow9am) }
                    )
                }
            }
        }

        // Bottom Block / Unblock action
        OutlinedButton(
            onClick = onToggleBlock,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CallMissedRed.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_block_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = null,
                tint = CallMissedRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isBlocked) strings.unblock else strings.markSpamAndBlock,
                color = CallMissedRed,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileQuickCircleAction(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRoundedRect: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = if (isRoundedRect) RoundedCornerShape(16.dp) else CircleShape,
            color = containerColor,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ReminderPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}
