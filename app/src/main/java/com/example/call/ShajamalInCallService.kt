package com.example.call

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.CallDirection
import com.example.data.model.SimSlotInfo
import com.example.data.preferences.PrivacyVaultDataStore
import com.example.data.preferences.SettingsDataStore
import com.example.data.telephony.CallerIntelligenceEngine
import com.example.data.telephony.DeviceDirectoryRepository
import com.example.data.telephony.DualSimCallManager
import com.example.data.telephony.PhoneNumberInfoHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShajamalInCallService : InCallService() {

    private var activeCall: Call? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsStore: SettingsDataStore
    private lateinit var privacyVaultStore: PrivacyVaultDataStore
    private lateinit var dualSimManager: DualSimCallManager
    private lateinit var directoryRepository: DeviceDirectoryRepository

    private var customMediaPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var cachedSimSlots: List<SimSlotInfo> = emptyList()

    private var currentCallIncoming: Boolean = false
    private var currentCallConnectedAtMillis: Long = 0L
    private var currentCallSimSlot: Int? = null
    private var loggedHiddenCallForCurrent: Boolean = false

    @Volatile
    private var maskedNotificationTitle: String? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state != Call.STATE_RINGING) {
                stopCustomRingtone()
                releaseIncomingWakeLock()
            }
            if (state == Call.STATE_ACTIVE && currentCallConnectedAtMillis == 0L) {
                currentCallConnectedAtMillis = System.currentTimeMillis()
            }
            CallRepository.refreshState(state)
            if (state == Call.STATE_DISCONNECTED) {
                maybeRecordHiddenCall(call)
                cancelCallNotification()
                releaseIncomingWakeLock()
            } else {
                postCallNotification(call, state)
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallRepository.refreshState()
            resolveCallerAsync(call)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsDataStore(applicationContext)
        privacyVaultStore = PrivacyVaultDataStore(applicationContext)
        dualSimManager = DualSimCallManager(applicationContext)
        directoryRepository = DeviceDirectoryRepository(applicationContext, dualSimManager)

        CallRepository.attachService(this)
        createNotificationChannels()

        serviceScope.launch(Dispatchers.IO) {
            cachedSimSlots = runCatching { dualSimManager.loadSimSlots() }.getOrDefault(emptyList())
            runCatching { settingsStore.syncFastCallCache() }
        }
    }

    override fun onDestroy() {
        stopCustomRingtone()
        releaseIncomingWakeLock()
        CallRepository.detachService(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallRepository.updateAudioState(audioState)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        val incomingNumber = call.details?.handle?.schemeSpecificPart.orEmpty()

        if (state == Call.STATE_RINGING) {
            acquireIncomingWakeLock()

            // Fast synchronous block check so call handling runs with 0ms coroutine/disk latency
            if (incomingNumber.isNotBlank() && settingsStore.isNumberBlockedFast(incomingNumber)) {
                releaseIncomingWakeLock()
                runCatching { call.reject(false, null) }
                return
            }

            // If fast prefs weren't seeded yet on first upgrade launch, verify DataStore asynchronously
            if (incomingNumber.isNotBlank() && !settingsStore.isFastCacheReady()) {
                serviceScope.launch(Dispatchers.IO) {
                    runCatching { settingsStore.syncFastCallCache() }
                    val blockedInDataStore = runCatching { settingsStore.isNumberBlocked(incomingNumber) }.getOrDefault(false)
                    if (blockedInDataStore) {
                        serviceScope.launch(Dispatchers.Main.immediate) {
                            stopCustomRingtone()
                            releaseIncomingWakeLock()
                            runCatching { call.reject(false, null) }
                        }
                    }
                }
            }

            val slotIndex = resolveSimSlotIndex(call)
            val customUriStr = settingsStore.getRingtoneUriFast(slotIndex)
            val playingCustom = if (!customUriStr.isNullOrBlank()) {
                startCustomRingtone(Uri.parse(customUriStr))
            } else {
                false
            }
            proceedWithCall(call, state, muteChannelRingtone = playingCustom)
        } else {
            proceedWithCall(call, state, muteChannelRingtone = false)
        }
    }

    private fun proceedWithCall(call: Call, initialState: Int, muteChannelRingtone: Boolean) {
        activeCall?.unregisterCallback(callCallback)
        activeCall = call
        call.registerCallback(callCallback)

        currentCallIncoming = (initialState == Call.STATE_RINGING)
        currentCallConnectedAtMillis = if (initialState == Call.STATE_ACTIVE) System.currentTimeMillis() else 0L
        currentCallSimSlot = resolveSimSlotIndex(call)
        loggedHiddenCallForCurrent = false
        maskedNotificationTitle = null

        val simLabel = resolveSimLabel(call)
        CallRepository.setCall(call, simLabel, initialState)
        postCallNotification(call, initialState, muteChannelRingtone = muteChannelRingtone)
        launchCallActivity()
        resolveCallerAsync(call)
    }

    private fun launchCallActivity() {
        val launchIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                .toBundle()
            runCatching { startActivity(launchIntent, options) }
                .onFailure { runCatching { startActivity(launchIntent) } }
        } else {
            runCatching { startActivity(launchIntent) }
        }
    }

    private fun resolveCallerAsync(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (number.isBlank()) return
        serviceScope.launch {
            val (name, photoUri) = directoryRepository.resolveCallerIdentity(number)
            CallRepository.updateResolvedCallerIdentity(number, name, photoUri)

            val settings = runCatching { settingsStore.settingsFlow.first() }.getOrNull()
            val privates = runCatching { privacyVaultStore.privateContactsFlow.first() }.getOrDefault(emptyList())
            val canonical = PhoneNumberInfoHelper.canonicalKey(number)
            val privateEntry = privates.firstOrNull {
                val pk = PhoneNumberInfoHelper.canonicalKey(it.number)
                (canonical.isNotBlank() && pk == canonical) || it.number == number
            }
            if (privateEntry != null && (privateEntry.sensitiveMask || settings?.sensitiveContactProtectionEnabled == true)) {
                val masked = CallerIntelligenceEngine.maskPhoneNumber(
                    PhoneNumberInfoHelper.formatDisplayNumber(number).ifBlank { number }
                )
                val lang = settings?.language ?: settingsStore.getLanguageFast()
                val label = if (lang == AppLanguage.BENGALI) {
                    "গোপন কন্টাক্ট ($masked)"
                } else {
                    "Protected Contact ($masked)"
                }
                maskedNotificationTitle = label
            }
            if (activeCall === call) {
                postCallNotification(call)
            }
        }
    }

    private fun maybeRecordHiddenCall(call: Call) {
        if (loggedHiddenCallForCurrent) return
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (number.isBlank()) return
        loggedHiddenCallForCurrent = true

        val wasIncoming = currentCallIncoming
        val connectedAt = currentCallConnectedAtMillis
        val simSlot = currentCallSimSlot
        val direction = when {
            wasIncoming && connectedAt > 0L -> CallDirection.INCOMING
            wasIncoming -> CallDirection.MISSED
            else -> CallDirection.OUTGOING
        }
        val durationSeconds = if (connectedAt > 0L) {
            ((System.currentTimeMillis() - connectedAt) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        CoroutineScope(Dispatchers.IO).launch {
            val privates = runCatching { privacyVaultStore.privateContactsFlow.first() }.getOrDefault(emptyList())
            val canonical = PhoneNumberInfoHelper.canonicalKey(number)
            val matchedPrivate = privates.firstOrNull {
                val pk = PhoneNumberInfoHelper.canonicalKey(it.number)
                (canonical.isNotBlank() && pk == canonical) || it.number == number
            }
            if (matchedPrivate != null) {
                privacyVaultStore.recordHiddenCall(
                    rawNumber = number,
                    contactName = matchedPrivate.name,
                    direction = direction,
                    timestampMillis = System.currentTimeMillis(),
                    durationSeconds = durationSeconds,
                    simSlotIndex = simSlot
                )
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (activeCall === call) {
            maybeRecordHiddenCall(call)
            stopCustomRingtone()
            releaseIncomingWakeLock()
            activeCall = null
            CallRepository.setCall(null, null)
            cancelCallNotification()
        }
    }

    private fun acquireIncomingWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        releaseIncomingWakeLock()
        runCatching {
            val flags = if (powerManager.isInteractive) {
                PowerManager.PARTIAL_WAKE_LOCK
            } else {
                @Suppress("DEPRECATION")
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            }
            val lock = powerManager.newWakeLock(flags, "Shajamal:IncomingCallWakeLock").apply {
                setReferenceCounted(false)
                acquire(12_000L)
            }
            wakeLock = lock
            serviceScope.launch {
                delay(11_500L)
                releaseIncomingWakeLock()
            }
        }
    }

    private fun releaseIncomingWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun startCustomRingtone(uri: Uri): Boolean {
        stopCustomRingtone()
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val mediaStarted = runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(this@ShajamalInCallService, uri)
                isLooping = true
                prepare()
                start()
            }
            customMediaPlayer = mp
            true
        }.getOrDefault(false)

        if (mediaStarted) return true

        return runCatching {
            val ringtone = RingtoneManager.getRingtone(this, uri) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.audioAttributes = audioAttrs
            ringtone.play()
            activeRingtone = ringtone
            true
        }.getOrDefault(false)
    }

    private fun stopCustomRingtone() {
        runCatching { customMediaPlayer?.stop() }
        runCatching { customMediaPlayer?.release() }
        customMediaPlayer = null
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    private fun getSimSlotsFast(): List<SimSlotInfo> {
        val existing = cachedSimSlots
        if (existing.isNotEmpty()) return existing
        val loaded = runCatching { dualSimManager.loadSimSlots() }.getOrDefault(emptyList())
        if (loaded.isNotEmpty()) cachedSimSlots = loaded
        return loaded
    }

    private fun resolveSimSlotIndex(call: Call): Int? {
        val accountHandle = call.details?.accountHandle ?: return null
        val slots = getSimSlotsFast()
        val matched = slots.firstOrNull { it.phoneAccountHandle == accountHandle }
            ?: slots.firstOrNull { it.phoneAccountHandle?.id == accountHandle.id }
        return matched?.slotIndex
    }

    private fun resolveSimLabel(call: Call): String? {
        val accountHandle = call.details?.accountHandle ?: return null
        val slots = getSimSlotsFast()
        val matched = slots.firstOrNull { it.phoneAccountHandle == accountHandle }
            ?: slots.firstOrNull { it.phoneAccountHandle?.id == accountHandle.id }
        return matched?.summaryLabel
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val incomingChannel = NotificationChannel(
                CHANNEL_INCOMING,
                getString(R.string.incoming_call_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
                val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                setSound(
                    ringtone,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            val incomingSilentChannel = NotificationChannel(
                CHANNEL_INCOMING_SILENT,
                "${getString(R.string.incoming_call_channel_name)} (Custom Tone)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
                setSound(null, null)
            }
            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING,
                getString(R.string.ongoing_call_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(incomingChannel)
            manager.createNotificationChannel(incomingSilentChannel)
            manager.createNotificationChannel(ongoingChannel)
        }
    }

    private fun postCallNotification(
        call: Call,
        explicitState: Int? = null,
        muteChannelRingtone: Boolean = false
    ) {
        val state = explicitState ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        val isRinging = state == Call.STATE_RINGING
        val channelId = when {
            isRinging && muteChannelRingtone -> CHANNEL_INCOMING_SILENT
            isRinging -> CHANNEL_INCOMING
            else -> CHANNEL_ONGOING
        }

        val repoState = CallRepository.activeCallState.value
        val caller = maskedNotificationTitle
            ?: repoState?.callerName?.takeIf { it.isNotBlank() }
            ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: call.details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
            ?: "Incoming Call"

        val callUiIntent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            callUiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerPendingIntent = PendingIntent.getBroadcast(
            this,
            1002,
            Intent(this, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_ANSWER_CALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            1003,
            Intent(this, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_DECLINE_CALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(caller)
            .setImportant(true)
            .build()

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(caller)
            .setContentText(if (isRinging) "Incoming call" else "Active call · Shajamal")
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(if (isRinging) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(fullScreenPendingIntent)

        runCatching {
            val callStyle = if (isRinging) {
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    declinePendingIntent,
                    answerPendingIntent
                )
            } else {
                NotificationCompat.CallStyle.forOngoingCall(
                    callerPerson,
                    declinePendingIntent
                )
            }
            builder.setStyle(callStyle)
        }

        if (isRinging) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        runCatching { manager?.notify(NOTIFICATION_ID, builder.build()) }
    }

    private fun cancelCallNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_INCOMING = "shajamal_incoming_calls_v2"
        private const val CHANNEL_INCOMING_SILENT = "shajamal_incoming_calls_silent_v2"
        private const val CHANNEL_ONGOING = "shajamal_ongoing_calls_v2"
        private const val NOTIFICATION_ID = 4201
    }
}
