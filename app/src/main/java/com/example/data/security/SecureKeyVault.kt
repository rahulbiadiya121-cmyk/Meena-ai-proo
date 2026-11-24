package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.domain.ApiProvider
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM vault for storing user-supplied API keys.
 * Keys are never written in plaintext or logged.
 */
class SecureKeyVault(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveKey(provider: ApiProvider, rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) {
            clearKey(provider)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(trimmed.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
        prefs.edit().putString(keyPrefName(provider), encoded).apply()
    }

    fun getKey(provider: ApiProvider): String? {
        val encoded = prefs.getString(keyPrefName(provider), null) ?: return null
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < 2) return null
            val ivLen = combined[0].toInt() and 0xFF
            if (combined.size <= 1 + ivLen) return null
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val ciphertext = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
            val plainBytes = cipher.doFinal(ciphertext)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasKey(provider: ApiProvider): Boolean = !getKey(provider).isNullOrBlank()

    fun getMaskedPreview(provider: ApiProvider): String {
        val key = getKey(provider) ?: return ""
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }

    fun clearKey(provider: ApiProvider) {
        prefs.edit().remove(keyPrefName(provider)).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyPrefName(provider: ApiProvider): String = "enc_key_${provider.id}"

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "meena_ai_pro_vault_master_key"
        private const val PREFS_NAME = "meena_secure_key_vault"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
