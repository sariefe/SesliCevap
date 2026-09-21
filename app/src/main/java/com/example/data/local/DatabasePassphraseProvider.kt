package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

/**
 * Device-bound SQLCipher passphrase protected by an AES key in Android Keystore.
 */
object DatabasePassphraseProvider {

    private const val PREFS_NAME = "db_secure_prefs"
    private const val KEY_PASSPHRASE_CIPHERTEXT = "db_passphrase_ciphertext"
    private const val KEY_PASSPHRASE_IV = "db_passphrase_iv"
    private const val KEY_ENCRYPTED_MIGRATED = "db_encrypted_v1"
    private const val KEYSTORE_ALIAS = "seslicevap_db_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PASSPHRASE_BYTES = 32

    fun getPassphrase(context: Context): ByteArray {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedCiphertext = prefs.getString(KEY_PASSPHRASE_CIPHERTEXT, null)
        val encodedIv = prefs.getString(KEY_PASSPHRASE_IV, null)

        if (encodedCiphertext != null && encodedIv != null) {
            return decryptPassphrase(
                Base64.decode(encodedCiphertext, Base64.NO_WRAP),
                Base64.decode(encodedIv, Base64.NO_WRAP)
            )
        }

        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val (ciphertext, iv) = encryptPassphrase(passphrase)
        prefs.edit {
            putString(KEY_PASSPHRASE_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
        }
        return passphrase
    }

    /**
     * One-time wipe of any pre-encryption plaintext DB so SQLCipher can take over cleanly.
     */
    fun migrateToEncryptedDatabaseIfNeeded(context: Context, databaseName: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENCRYPTED_MIGRATED, false)) return

        context.deleteDatabase(databaseName)
        prefs.edit { putBoolean(KEY_ENCRYPTED_MIGRATED, true) }
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(passphrase)
        return ciphertext to cipher.iv
    }

    private fun decryptPassphrase(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
