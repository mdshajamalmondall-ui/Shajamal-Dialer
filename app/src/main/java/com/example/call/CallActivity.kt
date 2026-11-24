package com.example.call

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.telecom.Call
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CallerIdentityReport
import com.example.data.model.CallerTrustStatus
import com.example.data.model.CommunitySpamReport
import com.example.data.model.DialerSettings
import com.example.data.model.PrivateContactEntry
import com.example.data.preferences.PrivacyVaultDataStore
import com.example.data.preferences.SettingsDataStore
import com.example.data.telephony.CallerIntelligenceEngine
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
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        enableEdgeToEdge()
        val settingsDataStore = SettingsDataStore(applicationContext)
        val privacyVaultDataStore = PrivacyVaultDataStore(applicationContext)

        setContent {
            val initialSettings = remember { settingsDataStore.getFastSettings() }
            val settings by settingsDataStore.settingsFlow.collectAsState(initial = initialSettings)
            val blockedNumbers by settingsDataStore.blockedNumbersFlow.collectAsState(initial = emptyList())
            val communityReports by settingsDataStore.communityReportsFlow.collectAsState(initial = emptyMap())
            val numberNotes by settingsDataStore.numberNotesFlow.collectAsState(initial = emptyMap())
            val privateContacts by privacyVaultDataStore.privateContactsFlow.collectAsState(initial = emptyList())
            val activeCall by CallRepository.activeCallState.collectAsState()
            val theme = ThemeCatalog.byId(settings.selectedThemeId)

            LaunchedEffect(activeCall) {
                if (activeCall == null) {
                    delay(350)
                    if (CallRepository.activeCallState.value == null) {
                        finishAndRemoveTask()
                    }
                }
            }

            ShajamalTheme(
                pictureTheme = theme,
                language = settings.language,
                displayScale = settings.displayScale,
                fontScale = settings.fontScale
            ) {
                ThemeBackdrop(
                    theme = theme,
                    doodleDesign = settings.selectedDoodle,
                    customImagePath = settings.customThemeImagePath,
                    dimAlpha = settings.overlayDimAlpha
                ) {
                    activeCall?.let { callState ->
                        val canonical = PhoneNumberInfoHelper.canonicalKey(callState.number)
                        val note = if (canonical.isNotBlank()) numberNotes[canonical] else null
                        val callerReport = remember(
                            callState.number,
                            callState.callerName,
                            callState.photoUri,
                            note,
                            blockedNumbers,
                            communityReports,
                            privateContacts,
                            settings
                        ) {
                            CallerIntelligenceEngine.evaluate(
                                rawNumber = callState.number,
                                contactName = callState.callerName,
                                photoUri = callState.photoUri,
                                note = note,
                                blockedNumbers = blockedNumbers,
                                communityReports = communityReports,
                                privateContacts = privateContacts,
                                settings = settings
                            )
                        }
                        InCallScreen(
                            callState = callState,
                            callerReport = callerReport,
                            showCallerIdPill = settings.callerIdEnabled
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun InCallScreen(
    callState: ActiveCallUiState,
    callerReport: CallerIdentityReport,
    showCallerIdPill: Boolean
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var showKeypad by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var showRemindDialog by remember { mutableStateOf(false) }
    var showQuickMessageDialog by remember { mutableStateOf(false) }

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

    val visibleCallerTitle = if (callerReport.isSensitiveMasked) {
        callerReport.displayName
    } else {
        callState.callerName ?: if (callerReport.isBusinessVerified) callerReport.displayName else callState.number
    }
    val visiblePhotoUri = if (callerReport.isSensitiveMasked) null else callState.photoUri
    val avatarText = (visibleCallerTitle.firstOrNull() ?: callState.number.firstOrNull() ?: '?')
        .uppercaseChar()
        .toString()

    val formattedNumber = callerReport.formattedNumber
    val carrierText = callerReport.carrierRegion.substringBefore(" ·")
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
                    text = visibleCallerTitle,
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

                val hideStandardSavedContactPill =
                    callerReport.status == CallerTrustStatus.VERIFIED_CONTACT && !callerReport.isPrivate
                if (showCallerIdPill && !hideStandardSavedContactPill) {
                    Spacer(Modifier.height(10.dp))
                    val pillColor = when (callerReport.status) {
                        CallerTrustStatus.VERIFIED_BUSINESS, CallerTrustStatus.VERIFIED_CONTACT -> Color(0xFF22C55E)
                        CallerTrustStatus.FRAUD_WARNING, CallerTrustStatus.BLOCKED -> Color(0xFFEF4444)
                        CallerTrustStatus.SPAM_SUSPECTED -> Color(0xFFF59E0B)
                        CallerTrustStatus.SENSITIVE_PROTECTED -> Color(0xFF818CF8)
                        CallerTrustStatus.UNKNOWN_CLEAN -> Color(0xFF38BDF8)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = callerReport.statusBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Centerpiece: Continuously rotating orbital animation until answered, OR DTMF Keypad
            if (!showKeypad) {
                if (isDialingOrRinging) {
                    CallingOrbitAnimation(
                        centerText = avatarText,
                        accentColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = Color(0xFFFDE68A),
                        size = 250.dp,
                        photoUri = visiblePhotoUri
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.55f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarText,
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 48.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (!visiblePhotoUri.isNullOrBlank()) {
                            val photoRequest = remember(visiblePhotoUri) {
                                ImageRequest.Builder(context)
                                    .data(visiblePhotoUri)
                                    .size(360)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = photoRequest,
                                contentDescription = visibleCallerTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Remind Me & Message row matching Screenshot
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IncomingActionIconLabel(
                                icon = Icons.Rounded.Alarm,
                                label = strings.remindMe,
                                onClick = { showRemindDialog = true },
                                modifier = Modifier.testTag("in_call_remind_me_button")
                            )
                            IncomingActionIconLabel(
                                icon = Icons.Rounded.ChatBubble,
                                label = strings.messageLabel,
                                onClick = { showQuickMessageDialog = true },
                                modifier = Modifier.testTag("in_call_message_button")
                            )
                        }

                        // Slide to answer pill bar
                        SlideToAnswerBar(
                            label = strings.slideToAnswer,
                            onAnswer = { CallRepository.answerCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("in_call_slide_to_answer")
                        )

                        // Quick Decline pill button
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CallEndRed.copy(alpha = 0.92f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { CallRepository.rejectOrDisconnect() }
                                .testTag("in_call_decline_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = strings.declineCall,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.declineCall,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
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

    if (showRemindDialog) {
        val options = listOf(
            strings.remind15m to (15L * 60L * 1000L),
            strings.remind30m to (30L * 60L * 1000L),
            strings.remind1h to (60L * 60L * 1000L),
            strings.remindTomorrow9am to null
        )
        AlertDialog(
            onDismissRequest = { showRemindDialog = false },
            title = { Text(strings.remindMe) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (label, offset) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showRemindDialog = false
                                    val startMillis = if (offset == null) {
                                        Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, 9)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    } else {
                                        System.currentTimeMillis() + offset
                                    }
                                    scope.launch {
                                        SettingsDataStore(context.applicationContext)
                                            .saveNumberNote(callState.number, "Reminder ($label)")
                                    }
                                    CallRepository.rejectOrDisconnect()
                                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(
                                            CalendarContract.Events.TITLE,
                                            "Call back ${callState.callerName ?: callState.number}"
                                        )
                                        putExtra(
                                            CalendarContract.Events.DESCRIPTION,
                                            "Phone: ${callState.number}"
                                        )
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                                        putExtra(
                                            CalendarContract.EXTRA_EVENT_END_TIME,
                                            startMillis + 15L * 60L * 1000L
                                        )
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    runCatching { context.startActivity(calIntent) }
                                    Toast.makeText(
                                        context,
                                        "Reminder saved: $label",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRemindDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showQuickMessageDialog) {
        val quickReplies = listOf(
            "I'll call you right back.",
            "In a meeting right now, please text.",
            "Can't talk at the moment.",
            "On my way!"
        )
        var customReply by remember { mutableStateOf("") }

        fun sendReplyAndDecline(message: String) {
            val trimmed = message.trim()
            if (trimmed.isBlank()) return
            showQuickMessageDialog = false
            val rejected = CallRepository.rejectWithMessage(trimmed)
            if (!rejected) {
                CallRepository.rejectOrDisconnect()
                val smsIntent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("smsto:${Uri.encode(callState.number)}")
                ).apply {
                    putExtra("sms_body", trimmed)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(smsIntent) }
            }
            Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
        }

        AlertDialog(
            onDismissRequest = { showQuickMessageDialog = false },
            title = { Text(strings.quickReplyPrompt) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickReplies.forEach { reply ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { sendReplyAndDecline(reply) }
                        ) {
                            Text(
                                text = reply,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customReply,
                        onValueChange = { customReply = it },
                        placeholder = { Text(strings.notePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { sendReplyAndDecline(customReply) },
                    enabled = customReply.isNotBlank()
                ) {
                    Text(strings.smsLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickMessageDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun IncomingActionIconLabel(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SlideToAnswerBar(
    label: String,
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var answered by remember { mutableStateOf(false) }

    val shimmerTransition = rememberInfiniteTransition(label = "slide_shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.26f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), RoundedCornerShape(50))
            .padding(5.dp)
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val thumbSize = 58.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val maxOffsetPx = (trackWidthPx - thumbSizePx).coerceAtLeast(1f)

        var rawOffsetPx by remember { mutableFloatStateOf(0f) }
        val animatedOffsetPx by animateFloatAsState(
            targetValue = rawOffsetPx,
            animationSpec = spring(),
            label = "slider_thumb"
        )

        val progress = (animatedOffsetPx / maxOffsetPx).coerceIn(0f, 1f)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.alpha((1f - progress * 1.4f).coerceIn(0.15f, 1f) * shimmerAlpha)
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .size(thumbSize)
                .pointerInput(maxOffsetPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!answered) {
                                if (rawOffsetPx >= maxOffsetPx * 0.72f) {
                                    answered = true
                                    rawOffsetPx = maxOffsetPx
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAnswer()
                                } else {
                                    rawOffsetPx = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            if (!answered) rawOffsetPx = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!answered) {
                                rawOffsetPx = (rawOffsetPx + dragAmount).coerceIn(0f, maxOffsetPx)
                                if (rawOffsetPx >= maxOffsetPx * 0.92f) {
                                    answered = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAnswer()
                                }
                            }
                        }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = label,
                    tint = CallIncomingGreen,
                    modifier = Modifier.size(28.dp)
                )
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
