package com.izuuhh.mira.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the Spotify refresh token encrypted with a key that never leaves the Android Keystore. */
class TokenStore(context: Context) {
    private val sp = context.getSharedPreferences("mira_tokens", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun write(token: String) {
        runCatching {
            val c = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
            val data = c.iv + c.doFinal(token.toByteArray())
            sp.edit().putString("refresh", Base64.encodeToString(data, Base64.NO_WRAP)).apply()
        }
    }

    fun read(): String? = runCatching {
        val data = Base64.decode(sp.getString("refresh", null) ?: return null, Base64.NO_WRAP)
        val c = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, 12))
        }
        String(c.doFinal(data, 12, data.size - 12))
    }.getOrNull()

    fun clear() = sp.edit().remove("refresh").apply()

    private companion object {
        const val ALIAS = "mira_refresh_token"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
