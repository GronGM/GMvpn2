package com.gmvpn.client.profile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption with the symmetric key kept inside
 * [AndroidKeyStore][KeyStore.getInstance]. We never extract the raw
 * key bytes — encrypt/decrypt go through the keystore, so even a
 * privileged attacker that reads `profileDataStore` off disk cannot
 * recover the plaintext URI without exploiting the device's
 * StrongBox / TEE.
 *
 * Wire format of [encrypt] output (Base64 NO_WRAP):
 *
 *   ┌──────────┬──────────────────┐
 *   │ IV (12B) │ ciphertext + tag │
 *   └──────────┴──────────────────┘
 *
 * IV is generated freshly per encrypt by `AES/GCM/NoPadding`'s
 * default randomized IV (12 bytes).
 */
internal class KeystoreSecrets(private val alias: String = DEFAULT_ALIAS) {

    fun encrypt(plain: String): String {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        require(iv.size == GCM_IV_BYTES) { "unexpected IV length: ${iv.size}" }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ct.size).also {
            System.arraycopy(iv, 0, it, 0, iv.size)
            System.arraycopy(ct, 0, it, iv.size, ct.size)
        }
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? {
        val combined = try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (combined.size <= GCM_IV_BYTES + GCM_TAG_BYTES) return null
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val ct = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val key = getKey() ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(GCM_TAG_BYTES * 8, iv),
            )
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun getKey(): SecretKey? {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: return null
        return entry.secretKey
    }

    private fun getOrCreateKey(): SecretKey {
        getKey()?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "gmvpn.profile.v1"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BYTES = 16
        private const val KEY_SIZE_BITS = 256
    }
}
