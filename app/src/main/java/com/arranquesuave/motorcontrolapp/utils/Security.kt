// app/src/main/java/com/arranquesuave/motorcontrolapp/util/Security.kt
package com.arranquesuave.motorcontrolapp.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Security {
    private const val PREFS_NAME = "motor_prefs"
    private const val KEY_ALIAS   = "motor-key"

    /** Genera la clave AES/GCM en Android Keystore (API ≥ 23) */
    fun initKey() {
        val kg = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        kg.init(spec)
        kg.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    /** Cifra datos y devuelve IV + ciphertext */
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher.iv + cipher.doFinal(data)
    }

    /** Descifra datos que contienen IV al inicio */
    fun decrypt(blob: ByteArray): ByteArray {
        val iv     = blob.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(blob.copyOfRange(12, blob.size))
    }

    /** SharedPrefs cifradas (opcional para credenciales) */
    fun getEncryptedPrefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx, PREFS_NAME,
        MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
