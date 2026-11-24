package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DialerSettings
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun PrivacyDashboardScreen(
    settings: DialerSettings,
    isDefaultDialer: Boolean,
    blockedCount: Int,
    spamReportCount: Int,
    privateContactCount: Int,
    hiddenCallCount: Int,
    onBack: () -> Unit,
    onOpenPrivateContacts: () -> Unit,
    onOpenHiddenHistory: () -> Unit,
    onOpenSpamDatabase: () -> Unit,
    onToggleCallerId: (Boolean) -> Unit,
    onToggleSpamDetection: (Boolean) -> Unit,
    onToggleFraudWarning: (Boolean) -> Unit,
    onToggleAutoSpamCategory: (Boolean) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onTogglePinProtectedSettings: (Boolean) -> Unit,
    onToggleSensitiveProtection: (Boolean) -> Unit,
    onSetSecurityPin: (String) -> Unit,
    onRemoveSecurityPin: () -> Unit,
    onExportBackup: (Uri, String) -> Unit,
    onRestoreBackup: (Uri, String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pendingPinActionAfterSet by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showBackupPassphraseDialog by remember { mutableStateOf<BackupActionMode?>(null) }
    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            pendingBackupUri = uri
            showBackupPassphraseDialog = BackupActionMode.EXPORT
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingBackupUri = uri
            showBackupPassphraseDialog = BackupActionMode.RESTORE
        }
    }

    val enabledScoreItems = listOf(
        isDefaultDialer,
        settings.callerIdEnabled,
        settings.spamDetectionEnabled,
        settings.fraudWarningEnabled,
        settings.autoSpamCategoryEnabled,
        settings.hasSecurityPin,
        settings.appLockEnabled || settings.pinProtectedSettingsEnabled,
        settings.sensitiveContactProtectionEnabled
    ).count { it }
    val scorePercent = ((enabledScoreItems * 100f) / 8f).toInt().coerceIn(0, 100)

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
                    modifier = Modifier.testTag("privacy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.privacyDashboardTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Protection Score Overview Card
        item {
            val shieldColor = when {
                scorePercent >= 75 -> CallIncomingGreen
                scorePercent >= 45 -> Color(0xFFD97706)
                else -> CallMissedRed
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, shieldColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = shieldColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = shieldColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.protectionScoreLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.privacyDashboardSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "$scorePercent%",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = shieldColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { scorePercent / 100f },
                        color = shieldColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        // Vault & Databases Quick Navigation Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrivacyNavTile(
                    icon = Icons.Rounded.PersonOff,
                    tint = Color(0xFF6366F1),
                    title = strings.privateContactsTitle,
                    subtitle = strings.privateContactsSub,
                    badgeCount = privateContactCount,
                    onClick = onOpenPrivateContacts,
                    testTag = "open_private_contacts_button"
                )
                PrivacyNavTile(
                    icon = Icons.Rounded.History,
                    tint = Color(0xFF0EA5E9),
                    title = strings.hiddenHistoryTitle,
                    subtitle = strings.hiddenHistorySub,
                    badgeCount = hiddenCallCount,
                    onClick = onOpenHiddenHistory,
                    testTag = "open_hidden_history_button"
                )
                PrivacyNavTile(
                    icon = Icons.Rounded.ReportProblem,
                    tint = CallMissedRed,
                    title = strings.spamDatabaseTitle,
                    subtitle = strings.spamDatabaseSub,
                    badgeCount = blockedCount + spamReportCount,
                    onClick = onOpenSpamDatabase,
                    testTag = "open_spam_database_button"
                )
            }
        }

        // Caller ID & Spam/Fraud Controls
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = CallIncomingGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.callerIdTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    PrivacySwitchRow(
                        title = strings.callerIdTitle,
                        subtitle = strings.callerIdSub,
                        checked = settings.callerIdEnabled,
                        onCheckedChange = onToggleCallerId
                    )
                    PrivacySwitchRow(
                        title = strings.spamDetectionTitle,
                        subtitle = strings.spamDetectionSub,
                        checked = settings.spamDetectionEnabled,
                        onCheckedChange = onToggleSpamDetection
                    )
                    PrivacySwitchRow(
                        title = strings.fraudWarningTitle,
                        subtitle = strings.fraudWarningSub,
                        checked = settings.fraudWarningEnabled,
                        onCheckedChange = onToggleFraudWarning
                    )
                    PrivacySwitchRow(
                        title = strings.autoSpamCategoryTitle,
                        subtitle = strings.autoSpamCategorySub,
                        checked = settings.autoSpamCategoryEnabled,
                        onCheckedChange = onToggleAutoSpamCategory
                    )
                }
            }
        }

        // App Lock, Fingerprint, PIN Protected Settings & Sensitive Contact Protection
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.appLockTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    PrivacySwitchRow(
                        title = strings.appLockTitle,
                        subtitle = strings.appLockSub,
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !settings.hasSecurityPin) {
                                pendingPinActionAfterSet = { onToggleAppLock(true) }
                                showPinSetupDialog = true
                            } else {
                                onToggleAppLock(enable)
                            }
                        }
                    )

                    PrivacySwitchRow(
                        title = strings.pinProtectedSettingsTitle,
                        subtitle = strings.pinProtectedSettingsSub,
                        checked = settings.pinProtectedSettingsEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !settings.hasSecurityPin) {
                                pendingPinActionAfterSet = { onTogglePinProtectedSettings(true) }
                                showPinSetupDialog = true
                            } else {
                                onTogglePinProtectedSettings(enable)
                            }
                        }
                    )

                    PrivacySwitchRow(
                        title = strings.sensitiveProtectionTitle,
                        subtitle = strings.sensitiveProtectionSub,
                        checked = settings.sensitiveContactProtectionEnabled,
                        onCheckedChange = onToggleSensitiveProtection
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingPinActionAfterSet = null
                                showPinSetupDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("privacy_set_pin_button")
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (settings.hasSecurityPin) strings.changeSecurityPin else strings.setSecurityPin)
                        }

                        if (settings.hasSecurityPin) {
                            OutlinedButton(
                                onClick = onRemoveSecurityPin,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("privacy_remove_pin_button")
                            ) {
                                Text(strings.removeSecurityPin, color = CallMissedRed)
                            }
                        }
                    }
                }
            }
        }

        // Secure Contact Backup (AES-256 Encrypted Export / Restore via SAF)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color(0xFF0EA5E9)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.secureBackupTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.secureBackupSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("shajamal_vault_backup_${System.currentTimeMillis()}.json")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_encrypted_backup_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.exportEncryptedBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_encrypted_backup_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restoreEncryptedBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (showPinSetupDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showPinSetupDialog = false
                pendingPinActionAfterSet = null
            },
            title = { Text(if (settings.hasSecurityPin) strings.changeSecurityPin else strings.setSecurityPin) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin1 = it },
                        label = { Text(strings.enterPinPrompt) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin2 = it },
                        label = { Text(strings.confirmPinPrompt) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin1.length != 4 || pin1 != pin2) {
                            onShowMessage("Enter matching 4-digit PINs")
                            return@Button
                        }
                        onSetSecurityPin(pin1)
                        pendingPinActionAfterSet?.invoke()
                        pendingPinActionAfterSet = null
                        showPinSetupDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinSetupDialog = false
                        pendingPinActionAfterSet = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    showBackupPassphraseDialog?.let { mode ->
        var passphrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showBackupPassphraseDialog = null
                pendingBackupUri = null
            },
            title = {
                Text(
                    if (mode == BackupActionMode.EXPORT) strings.exportEncryptedBackup
                    else strings.restoreEncryptedBackup
                )
            },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(strings.backupPassphraseLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingBackupUri
                        if (passphrase.length < 4) {
                            onShowMessage("Passphrase must be at least 4 characters")
                            return@Button
                        }
                        if (uri != null) {
                            if (mode == BackupActionMode.EXPORT) {
                                onExportBackup(uri, passphrase)
                            } else {
                                onRestoreBackup(uri, passphrase)
                            }
                        }
                        showBackupPassphraseDialog = null
                        pendingBackupUri = null
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupPassphraseDialog = null
                        pendingBackupUri = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

private enum class BackupActionMode { EXPORT, RESTORE }

@Composable
private fun PrivacyNavTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    badgeCount: Int,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = tint.copy(alpha = 0.14f)
            ) {
                Text(
                    text = badgeCount.toString(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacySwitchRow(
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
