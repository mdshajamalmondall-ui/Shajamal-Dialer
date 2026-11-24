package com.example.call

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.data.telephony.PhoneNumberInfoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class ActiveCallUiState(
    val number: String,
    val callerName: String?,
    val photoUri: String? = null,
    val simLabel: String?,
    val callState: Int,
    val connectTimeMillis: Long,
    val isMuted: Boolean,
    val isSpeakerOn: Boolean,
    val isOnHold: Boolean,
    val isRecording: Boolean = false,
    val lastRecordingPath: String? = null
)

object CallRepository {
    private val _activeCallState = MutableStateFlow<ActiveCallUiState?>(null)
    val activeCallState: StateFlow<ActiveCallUiState?> = _activeCallState.asStateFlow()

    private var currentCall: Call? = null
    private var inCallService: InCallService? = null
    private var currentSimLabel: String? = null
    private var isMuted: Boolean = false
    private var isSpeakerOn: Boolean = false
    private var activeEnteredAtMillis: Long = 0L

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false
    private var currentRecordingFile: File? = null
    private var lastRecordingPath: String? = null

    private var pendingCanonicalKey: String? = null
    private var resolvedCallerName: String? = null
    private var resolvedPhotoUri: String? = null

    fun attachService(service: InCallService) {
        inCallService = service
    }

    fun detachService(service: InCallService) {
        if (inCallService === service) {
            inCallService = null
        }
    }

    fun updateAudioState(audioState: CallAudioState?) {
        if (audioState == null) return
        isMuted = audioState.isMuted
        isSpeakerOn = (audioState.route and CallAudioState.ROUTE_SPEAKER) != 0
        refreshState()
    }

    fun setPendingOutgoingCaller(number: String, callerName: String?, photoUri: String?) {
        val key = PhoneNumberInfoHelper.canonicalKey(number)
        if (key.isNotBlank()) {
            pendingCanonicalKey = key
            resolvedCallerName = callerName
            resolvedPhotoUri = photoUri
        }
    }

    fun updateResolvedCallerIdentity(number: String, callerName: String?, photoUri: String?) {
        val currentNum = currentCall?.details?.handle?.schemeSpecificPart.orEmpty()
        val currentKey = PhoneNumberInfoHelper.canonicalKey(currentNum)
        val updateKey = PhoneNumberInfoHelper.canonicalKey(number)
        if (updateKey.isNotBlank() && (currentKey == updateKey || pendingCanonicalKey == updateKey)) {
            if (!callerName.isNullOrBlank()) resolvedCallerName = callerName
            if (!photoUri.isNullOrBlank()) resolvedPhotoUri = photoUri
            refreshState()
        }
    }

    fun setCall(call: Call?, simLabel: String?, initialState: Int? = null) {
        if (call == null) {
            stopRecordingInternal()
            activeEnteredAtMillis = 0L
            pendingCanonicalKey = null
            resolvedCallerName = null
            resolvedPhotoUri = null
        } else if (call !== currentCall) {
            activeEnteredAtMillis = 0L
            val handle = call.details?.handle?.schemeSpecificPart.orEmpty()
            val callKey = PhoneNumberInfoHelper.canonicalKey(handle)
            if (callKey.isBlank() || callKey != pendingCanonicalKey) {
                pendingCanonicalKey = callKey
                resolvedCallerName = null
                resolvedPhotoUri = null
            }
        }
        currentCall = call
        currentSimLabel = simLabel
        refreshState(initialState)
    }

    fun refreshState(explicitState: Int? = null) {
        val call = currentCall
        if (call == null) {
            _activeCallState.value = null
            return
        }

        val details = call.details
        val handle = details?.handle?.schemeSpecificPart.orEmpty()
        val callerDisplay = details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: resolvedCallerName

        val state = explicitState ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }

        if (state == Call.STATE_DISCONNECTED) {
            stopRecordingInternal()
            activeEnteredAtMillis = 0L
            pendingCanonicalKey = null
            resolvedCallerName = null
            resolvedPhotoUri = null
            _activeCallState.value = null
            currentCall = null
            return
        }

        if (state == Call.STATE_ACTIVE && activeEnteredAtMillis == 0L) {
            activeEnteredAtMillis = System.currentTimeMillis()
        } else if (state != Call.STATE_ACTIVE && state != Call.STATE_HOLDING) {
            activeEnteredAtMillis = 0L
        }

        val effectiveConnectTime = when {
            state != Call.STATE_ACTIVE && state != Call.STATE_HOLDING -> 0L
            (details?.connectTimeMillis ?: 0L) > 0L -> details!!.connectTimeMillis
            else -> activeEnteredAtMillis
        }

        _activeCallState.value = ActiveCallUiState(
            number = handle.ifBlank { "Unknown" },
            callerName = callerDisplay,
            photoUri = resolvedPhotoUri,
            simLabel = currentSimLabel,
            callState = state,
            connectTimeMillis = effectiveConnectTime,
            isMuted = isMuted,
            isSpeakerOn = isSpeakerOn,
            isOnHold = state == Call.STATE_HOLDING,
            isRecording = isRecording,
            lastRecordingPath = lastRecordingPath
        )
    }

    fun answerCall() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun rejectWithMessage(message: String): Boolean {
        stopRecordingInternal()
        val call = currentCall ?: return false
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        return if (state == Call.STATE_RINGING) {
            runCatching {
                call.reject(true, message)
                true
            }.getOrDefault(false)
        } else {
            false
        }
    }

    fun rejectOrDisconnect() {
        stopRecordingInternal()
        val call = currentCall ?: return
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        if (state == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    fun toggleMute() {
        val next = !isMuted
        isMuted = next
        inCallService?.setMuted(next)
        refreshState()
    }

    @Suppress("DEPRECATION")
    fun toggleSpeaker() {
        val next = !isSpeakerOn
        isSpeakerOn = next
        val route = if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
        inCallService?.setAudioRoute(route)
        refreshState()
    }

    fun toggleHold() {
        val call = currentCall ?: return
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.state ?: Call.STATE_NEW
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        if (state == Call.STATE_HOLDING) {
            call.unhold()
        } else if (state == Call.STATE_ACTIVE) {
            call.hold()
        }
    }

    fun toggleRecording(context: Context): Boolean {
        if (isRecording) {
            stopRecordingInternal()
            refreshState()
            return false
        }

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val outFile = File(dir, "shajamal_call_${System.currentTimeMillis()}.m4a")

        val started = tryStartRecorder(context, outFile, MediaRecorder.AudioSource.VOICE_COMMUNICATION) ||
            tryStartRecorder(context, outFile, MediaRecorder.AudioSource.MIC)

        if (!started) {
            runCatching { outFile.delete() }
            isRecording = false
            mediaRecorder = null
            refreshState()
            return false
        }

        currentRecordingFile = outFile
        isRecording = true
        refreshState()
        return true
    }

    private fun tryStartRecorder(context: Context, outFile: File, audioSource: Int): Boolean {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return runCatching {
            recorder.setAudioSource(audioSource)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            true
        }.getOrElse {
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            false
        }
    }

    private fun stopRecordingInternal() {
        val recorder = mediaRecorder
        mediaRecorder = null
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            currentRecordingFile?.takeIf { it.exists() && it.length() > 0L }?.let {
                lastRecordingPath = it.absolutePath
            }
        }
        currentRecordingFile = null
        isRecording = false
    }

    fun sendDtmf(digit: Char) {
        val call = currentCall ?: return
        call.playDtmfTone(digit)
        call.stopDtmfTone()
    }
}
