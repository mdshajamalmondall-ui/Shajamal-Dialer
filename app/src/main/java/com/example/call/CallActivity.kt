package com.example.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.Call
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.DialerSettings
import com.example.data.preferences.SettingsDataStore
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.components.CallingOrbitAnimation
import com.example.ui.components.RingingStatusPill
import com.example.ui.components.ThemeBackdrop
import com.example.ui.theme.CallEndRed
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShajamalTheme
import com.example.ui.theme.ThemeCatalog
import kotlinx.coroutines.delay

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsDataStore = SettingsDataStore(applicationContext)

        setContent {
            val settings by settingsDataStore.settingsFlow.collectAsState(initial = DialerSettings())
            val activeCall by CallRepository.activeCallState.collectAsState()
            val theme = ThemeCatalog.byId(settings.selectedThemeId)

            LaunchedEffect(activeCall) {
                if (activeCall == null) {
                    delay(350)
                    if (CallRepository.activeCallState.value == null) {
                        finish()
                    }
                }
            }

            ShajamalTheme(pictureTheme = theme, language = settings.language) {
                ThemeBackdrop(
                    theme = theme,
                    doodleDesign = settings.selectedDoodle,
                    customImagePath = settings.customThemeImagePath,
                    dimAlpha = settings.overlayDimAlpha
                ) {
                    activeCall?.let { callState ->
                        InCallScreen(callState = callState)
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallScreen(callState: ActiveCallUiState) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var showKeypad by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val started = CallRepository.toggleRecording(context)
            Toast.makeText(
                context,
                if (started) "Call recording started" else "Unable to start recording",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed to record calls",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val isConnectedActive = callState.callState == Call.STATE_ACTIVE && callState.connectTimeMillis > 0L

    LaunchedEffect(callState.callState, callState.connectTimeMillis) {
        if (isConnectedActive) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - callState.connectTimeMillis) / 1000L)
                    .coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val isDialingOrRinging = !isConnectedActive && callState.callState != Call.STATE_HOLDING

    val statusText = when {
        callState.callState == Call.STATE_RINGING -> strings.incomingCall
        callState.callState == Call.STATE_HOLDING -> strings.onHoldStatus
        isConnectedActive -> formatDuration(elapsedSeconds)
        callState.callState == Call.STATE_DIALING -> strings.ringingStatus
        else -> strings.ringingStatus
    }

    val avatarText = (callState.callerName?.firstOrNull() ?: callState.number.firstOrNull() ?: '?')
        .uppercaseChar()
        .toString()

    val formattedNumber = remember(callState.number) {
        PhoneNumberInfoHelper.formatDisplayNumber(callState.number)
    }
    val carrierText = remember(callState.number) {
        PhoneNumberInfoHelper.detectCarrierAndRegion(callState.number).substringBefore(" ·")
    }
    val subtitleLine = buildString {
        append(formattedNumber)
        if (!callState.simLabel.isNullOrBlank()) {
            append(" • ")
            append(callState.simLabel)
        }
        if (carrierText.isNotBlank()) {
            append(" • ")
            append(carrierText)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF476378).copy(alpha = 0.86f),
                        Color(0xFF4A543E).copy(alpha = 0.84f),
                        Color(0xFF1F2B28).copy(alpha = 0.92f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Pill + Recording Badge + Caller Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RingingStatusPill(statusText = statusText)

                    if (callState.isRecording) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CallEndRed.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "REC",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = callState.callerName ?: callState.number,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = subtitleLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )
            }

            // Centerpiece: Continuously rotating orbital animation until answered, OR DTMF Keypad
            if (!showKeypad) {
                if (isDialingOrRinging) {
                    CallingOrbitAnimation(
                        centerText = avatarText,
                        accentColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = Color(0xFFFDE68A),
                        size = 250.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarText,
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 48.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                AnimatedVisibility(visible = showKeypad) {
                    val dtmfRows = listOf(
                        listOf('1', '2', '3'),
                        listOf('4', '5', '6'),
                        listOf('7', '8', '9'),
                        listOf('*', '0', '#')
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        dtmfRows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                row.forEach { digit ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable { CallRepository.sendDtmf(digit) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = digit.toString(),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 24.sp,
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

            // Bottom Controls: 2x3 grid (Row 1: Speaker, Hold, Mute | Row 2: Record, End call, Keypad)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (callState.callState == Call.STATE_RINGING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CallEndRed,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .clickable { CallRepository.rejectOrDisconnect() }
                                .testTag("in_call_decline_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = "Decline call",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = CallIncomingGreen,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .clickable { CallRepository.answerCall() }
                                .testTag("in_call_answer_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = "Answer call",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        // Row 1: Speaker, Hold, Mute
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InCallCircleButton(
                                icon = Icons.Rounded.VolumeUp,
                                label = strings.speaker,
                                active = callState.isSpeakerOn,
                                onClick = { CallRepository.toggleSpeaker() }
                            )
                            InCallCircleButton(
                                icon = if (callState.isOnHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                label = if (callState.isOnHold) strings.resume else strings.hold,
                                active = callState.isOnHold,
                                onClick = { CallRepository.toggleHold() }
                            )
                            InCallCircleButton(
                                icon = if (callState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                label = if (callState.isMuted) strings.unmute else strings.mute,
                                active = callState.isMuted,
                                onClick = { CallRepository.toggleMute() }
                            )
                        }

                        // Row 2: Record, End Call (Center Red), Keypad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InCallCircleButton(
                                icon = if (callState.isRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                                label = if (callState.isRecording) strings.recording else strings.record,
                                active = callState.isRecording,
                                activeColor = CallEndRed,
                                activeIconTint = Color.White,
                                inactiveIconTint = Color(0xFFFCA5A5),
                                onClick = {
                                    val hasRecordPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasRecordPerm) {
                                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        val nowRecording = CallRepository.toggleRecording(context)
                                        Toast.makeText(
                                            context,
                                            if (nowRecording) "Recording call..." else "Recording saved",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = CallEndRed,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable { CallRepository.rejectOrDisconnect() }
                                        .testTag("in_call_end_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.CallEnd,
                                            contentDescription = strings.endCall,
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = strings.endCall,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }

                            InCallCircleButton(
                                icon = Icons.Rounded.Dialpad,
                                label = strings.keypad,
                                active = showKeypad,
                                onClick = { showKeypad = !showKeypad }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallCircleButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = Color.White,
    activeIconTint: Color = Color(0xFF1F2B28),
    inactiveIconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (active) activeColor else Color.White.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) activeIconTint else inactiveIconTint,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.88f)
        )
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
