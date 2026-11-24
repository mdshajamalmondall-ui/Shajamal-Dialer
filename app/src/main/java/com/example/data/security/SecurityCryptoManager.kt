package com.example.data.security

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Base64
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityCryptoManager {

    private const val PBKDF2_ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun generateSaltBase64(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        return Base64.encodeToString(derived, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, saltBase64: String, expectedHashBase64: String): Boolean {
        if (pin.isBlank() || saltBase64.isBlank() || expectedHashBase64.isBlank()) return false
        return runCatching {
            val actual = Base64.decode(hashPin(pin, saltBase64), Base64.NO_WRAP)
            val expected = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    fun encryptBackupJson(plainJson: String, passphrase: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        val keyBytes = deriveAesKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        return JSONObject().apply {
            put("app", "Shajamal")
            put("version", 1)
            put("algorithm", "AES-256-GCM")
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("payload", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }.toString(2)
    }

    fun decryptBackupJson(envelopeJson: String, passphrase: String): Result<String> {
        return runCatching {
            val obj = JSONObject(envelopeJson)
            val salt = Base64.decode(obj.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(obj.getString("iv"), Base64.NO_WRAP)
            val payload = Base64.decode(obj.getString("payload"), Base64.NO_WRAP)

            val keyBytes = deriveAesKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainBytes = cipher.doFinal(payload)
            String(plainBytes, Charsets.UTF_8)
        }
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}

object BiometricAuthHelper {

    fun canUseBiometrics(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java) ?: return false
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            } else {
                @Suppress("DEPRECATION")
                bm.canAuthenticate()
            }
            return res == BiometricManager.BIOMETRIC_SUCCESS
        }
        return true
    }

    fun promptFingerprint(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onUsePin: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !canUseBiometrics(activity)) {
            onUsePin()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val cancellationSignal = CancellationSignal()

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(
                negativeButtonText,
                executor,
                DialogInterface.OnClickListener { _, _ -> onUsePin() }
            )
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                    ) {
                        onUsePin()
                    } else {
                        onError(errString?.toString().orEmpty())
                    }
                }
            }
        )
    }
}
