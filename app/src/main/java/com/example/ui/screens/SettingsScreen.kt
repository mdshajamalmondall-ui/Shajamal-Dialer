package com.example.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.data.model.DualSimMode
import com.example.data.model.PermissionSnapshot
import com.example.data.model.SimSlotInfo
import com.example.ui.components.DisplayFontSettingsCard
import com.example.ui.components.DoodleCanvas
import com.example.ui.theme.AppPictureTheme
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme
import com.example.ui.theme.ThemeCatalog
import java.io.File

@Composable
fun SettingsScreen(
    settings: DialerSettings,
    simSlots: List<SimSlotInfo>,
    blockedCount: Int,
    isDefaultDialer: Boolean,
    permissions: PermissionSnapshot,
    onBack: () -> Unit,
    onOpenThemesGallery: () -> Unit,
    onOpenBlockList: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectDoodle: (DoodleDesign) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onPickSimRingtone: (Int, Uri?) -> Unit,
    onResetSimRingtone: (Int) -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRefreshSims: () -> Unit,
    onToggleDualSimMode: (DualSimMode) -> Unit,
    onToggleDtmfTones: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    onToggleAutoCleanPaste: (Boolean) -> Unit,
    onDisplayScaleChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activeTheme = LocalPictureTheme.current
    val strings = LocalAppStrings.current
    val isBn = settings.language == AppLanguage.BENGALI

    var pendingRingtoneSlot by remember { mutableIntStateOf(0) }
    var previewSlot by remember { mutableStateOf<Int?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
        previewSlot = null
    }

    fun togglePreview(slotIndex: Int, uriString: String?) {
        if (previewSlot == slotIndex) {
            stopPreview()
            return
        }
        stopPreview()
        val uri = if (!uriString.isNullOrBlank()) {
            Uri.parse(uriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        } ?: return

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener {
                    stopPreview()
                }
                prepare()
                start()
            }
            previewPlayer = mp
            previewSlot = slotIndex
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            stopPreview()
            onPickSimRingtone(pendingRingtoneSlot, uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Language Selector (English / বাংলা)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.languageTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.languageSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageOptionButton(
                            label = strings.languageEnglish,
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("language_english_button")
                        )
                        LanguageOptionButton(
                            label = strings.languageBengali,
                            selected = settings.language == AppLanguage.BENGALI,
                            onClick = { onSelectLanguage(AppLanguage.BENGALI) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("language_bengali_button")
                        )
                    }
                }
            }
        }

        // Display Size & Font Size Settings Card
        item {
            DisplayFontSettingsCard(
                displayScale = settings.displayScale,
                fontScale = settings.fontScale,
                onDisplayScaleChange = onDisplayScaleChange,
                onFontScaleChange = onFontScaleChange
            )
        }

        // Privacy & Caller Security Dashboard Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.42f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenPrivacyDashboard)
                    .testTag("open_privacy_dashboard_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF16A34A).copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color(0xFF15803D)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.privacyDashboardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.privacyDashboardSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = strings.privacyDashboardTitle,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Themes & Pictures Preview Strip Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenThemesGallery)
                            .testTag("open_picture_themes_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.themesTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${strings.activeThemePrefix}: ${if (isBn) activeTheme.titleBn else activeTheme.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = strings.themesTitle,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ThemeCatalog.themes, key = { it.id }) { themeItem ->
                            QuickThemeThumb(
                                theme = themeItem,
                                isBn = isBn,
                                selected = settings.selectedThemeId == themeItem.id,
                                customImagePath = settings.customThemeImagePath,
                                onClick = {
                                    if (themeItem.id == ThemeCatalog.ID_CUSTOM_PHOTO && settings.customThemeImagePath == null) {
                                        onOpenThemesGallery()
                                    } else {
                                        onSelectTheme(themeItem.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Doodle & Background Design Selector
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenThemesGallery),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.doodleSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBn) settings.selectedDoodle.titleBn else settings.selectedDoodle.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = strings.doodleSectionTitle,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(DoodleDesign.entries, key = { it.id }) { doodle ->
                            QuickDoodleThumb(
                                doodle = doodle,
                                theme = activeTheme,
                                isBn = isBn,
                                selected = settings.selectedDoodle == doodle,
                                onClick = { onSelectDoodle(doodle) }
                            )
                        }
                    }
                }
            }
        }

        // SIM 1 & SIM 2 Custom Ringtones (Audio picker via Storage Access Framework)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.ringtonesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.ringtonesSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SimRingtoneRow(
                        simLabel = simSlots.getOrNull(0)?.summaryLabel ?: "SIM 1",
                        toneName = settings.sim1RingtoneName ?: strings.defaultRingtone,
                        hasCustomTone = !settings.sim1RingtoneUri.isNullOrBlank(),
                        accent = activeTheme.sim1Accent,
                        isPlaying = previewSlot == 0,
                        chooseLabel = strings.chooseRingtone,
                        onChoose = {
                            pendingRingtoneSlot = 0
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        onPreviewToggle = { togglePreview(0, settings.sim1RingtoneUri) },
                        onReset = {
                            if (previewSlot == 0) stopPreview()
                            onResetSimRingtone(0)
                        },
                        testTag = "sim1_ringtone_choose_button"
                    )

                    SimRingtoneRow(
                        simLabel = simSlots.getOrNull(1)?.summaryLabel ?: "SIM 2",
                        toneName = settings.sim2RingtoneName ?: strings.defaultRingtone,
                        hasCustomTone = !settings.sim2RingtoneUri.isNullOrBlank(),
                        accent = activeTheme.sim2Accent,
                        isPlaying = previewSlot == 1,
                        chooseLabel = strings.chooseRingtone,
                        onChoose = {
                            pendingRingtoneSlot = 1
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        onPreviewToggle = { togglePreview(1, settings.sim2RingtoneUri) },
                        onReset = {
                            if (previewSlot == 1) stopPreview()
                            onResetSimRingtone(1)
                        },
                        testTag = "sim2_ringtone_choose_button"
                    )
                }
            }
        }

        // Block List Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, CallMissedRed.copy(alpha = 0.32f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenBlockList)
                    .testTag("open_block_list_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CallMissedRed.copy(alpha = 0.14f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = CallMissedRed
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.blockListTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.blockListSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CallMissedRed.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = blockedCount.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CallMissedRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = strings.blockListTitle,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Default Phone App & Permissions Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.defaultPhoneApp,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDefaultDialer) strings.defaultPhoneActiveSub else strings.defaultPhoneInactiveSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isDefaultDialer) {
                        Button(
                            onClick = onRequestDefaultDialer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_set_default_dialer_button")
                        ) {
                            Text(strings.setAsDefaultButton)
                        }
                    }

                    if (!permissions.hasCoreDialerPermissions || !permissions.readContacts || !permissions.readCallLog) {
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.grantAllPermissionsButton)
                        }
                    }
                }
            }
        }

        // Dual SIM Hardware Inspector
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.dualSimStatusTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onRefreshSims) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh SIM slots",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    simSlots.forEach { slot ->
                        val accent = if (slot.slotIndex == 0) activeTheme.sim1Accent else activeTheme.sim2Accent
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SimCard,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${slot.label} · ${slot.carrierName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (slot.phoneAccountHandle != null) {
                                            "Slot ${slot.slotIndex + 1} Bound (Sub ID: ${slot.subscriptionId})"
                                        } else {
                                            "No active SIM detected on Slot ${slot.slotIndex + 1}"
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialer Behavior Settings
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.dialerPreferencesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SettingsSwitchRow(
                        title = strings.askSimToggleTitle,
                        subtitle = strings.askSimToggleSub,
                        checked = settings.dualSimMode == DualSimMode.ASK_ON_CALL,
                        onCheckedChange = { checked ->
                            onToggleDualSimMode(
                                if (checked) DualSimMode.ASK_ON_CALL else DualSimMode.DUAL_BUTTONS
                            )
                        }
                    )

                    SettingsSwitchRow(
                        title = strings.dtmfTonesTitle,
                        subtitle = strings.dtmfTonesSub,
                        checked = settings.dtmfTonesEnabled,
                        onCheckedChange = onToggleDtmfTones
                    )

                    SettingsSwitchRow(
                        title = strings.hapticTitle,
                        subtitle = strings.hapticSub,
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = onToggleHapticFeedback
                    )

                    SettingsSwitchRow(
                        title = strings.autoCleanTitle,
                        subtitle = strings.autoCleanSub,
                        checked = settings.autoCleanPastedNumber,
                        onCheckedChange = onToggleAutoCleanPaste
                    )
                }
            }
        }
    }
}

@Composable
private fun SimRingtoneRow(
    simLabel: String,
    toneName: String,
    hasCustomTone: Boolean,
    accent: Color,
    isPlaying: Boolean,
    chooseLabel: String,
    onChoose: () -> Unit,
    onPreviewToggle: () -> Unit,
    onReset: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SimCard,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = simLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = toneName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasCustomTone) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPreviewToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Preview",
                        tint = accent
                    )
                }

                if (hasCustomTone) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Reset",
                            tint = CallMissedRed
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onChoose,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(chooseLabel, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LanguageOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickThemeThumb(
    theme: AppPictureTheme,
    isBn: Boolean,
    selected: Boolean,
    customImagePath: String?,
    onClick: () -> Unit
) {
    val label = if (isBn) theme.titleBn else theme.title
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        color = theme.colorScheme.background,
        modifier = Modifier
            .size(width = 76.dp, height = 104.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("settings_theme_thumb_${theme.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                theme.id == ThemeCatalog.ID_CUSTOM_PHOTO && !customImagePath.isNullOrBlank() -> {
                    val file = File(customImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                theme.backgroundRes != null -> {
                    Image(
                        painter = painterResource(id = theme.backgroundRes),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                theme.id == ThemeCatalog.ID_CUSTOM_PHOTO -> {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = label,
                        tint = theme.accentPrimary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = theme.accentPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        if (theme.isLight) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (theme.isLight) Color(0xFF0F172A) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickDoodleThumb(
    doodle: DoodleDesign,
    theme: AppPictureTheme,
    isBn: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = if (isBn) doodle.titleBn else doodle.titleEn
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        color = theme.colorScheme.background,
        modifier = Modifier
            .size(width = 88.dp, height = 88.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("settings_doodle_thumb_${doodle.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DoodleCanvas(
                design = doodle,
                tintColor = theme.accentSecondary,
                isLightTheme = theme.isLight,
                modifier = Modifier.fillMaxSize()
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        if (theme.isLight) Color.White.copy(alpha = 0.88f) else Color.Black.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (theme.isLight) Color(0xFF0F172A) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
