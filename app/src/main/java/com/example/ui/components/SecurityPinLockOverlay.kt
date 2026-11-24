package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.BiometricAuthHelper
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun SecurityPinLockOverlay(
    title: String,
    subtitle: String,
    allowDismiss: Boolean = false,
    onDismiss: () -> Unit = {},
    onVerifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val strings = LocalAppStrings.current
    val canBiometric = remember(context) { BiometricAuthHelper.canUseBiometrics(context) }

    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    fun triggerFingerprint() {
        if (activity == null || !canBiometric) return
        BiometricAuthHelper.promptFingerprint(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = strings.enterPinPrompt,
            onSuccess = onUnlocked,
            onUsePin = {},
            onError = { msg ->
                if (msg.isNotBlank()) errorText = msg
            }
        )
    }

    LaunchedEffect(Unit) {
        if (canBiometric) {
            triggerFingerprint()
        }
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4 && !checking) {
            checking = true
            val ok = onVerifyPin(pinInput)
            checking = false
            if (ok) {
                pinInput = ""
                errorText = null
                onUnlocked()
            } else {
                errorText = "Incorrect PIN"
                pinInput = ""
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        modifier = modifier
            .fillMaxSize()
            .testTag("security_pin_lock_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(26.dp))

                // 4 PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText ?: strings.enterPinPrompt,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (errorText != null) CallMissedRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Numeric PIN Pad
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { digit ->
                            PinPadDigitButton(
                                label = digit,
                                onClick = {
                                    if (pinInput.length < 4) {
                                        errorText = null
                                        pinInput += digit
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canBiometric) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable { triggerFingerprint() }
                                .testTag("lock_fingerprint_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = strings.useFingerprint,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(68.dp))
                    }

                    PinPadDigitButton(
                        label = "0",
                        onClick = {
                            if (pinInput.length < 4) {
                                errorText = null
                                pinInput += "0"
                            }
                        }
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                }
                            }
                            .testTag("lock_backspace_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (canBiometric) {
                    OutlinedButton(onClick = { triggerFingerprint() }) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.useFingerprint)
                    }
                }
                if (allowDismiss) {
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPadDigitButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_digit_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
