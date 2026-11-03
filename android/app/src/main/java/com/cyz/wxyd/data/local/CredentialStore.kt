package com.cyz.wxyd.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.credentialDataStore by preferencesDataStore(name = "wxyd_credential")

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "wxyd_auto_login_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/**
 * 自动登录凭据加密（AndroidKeyStore AES-GCM，无需用户验证窗口——
 * 与 [BiometricCrypto] 的区别：此密钥不做 setUserAuthenticationRequired，
 * App 启动即可静默解密重登；安全性由"密钥不出安全硬件、仅本机可解"保证，
 * 换设备/卸载重装后密钥不存在，密文即不可还原（永久登录但换设备失效）。
 */
object CredentialCrypto {

    /** 加密为 `Base64(iv ++ ciphertext)` 单串。 */
    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, obtainKey())
        }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ct, Base64.NO_WRAP)
    }

    /** 解密 [payload]；Keystore 密钥缺失/被清（换设备、清数据）时抛异常。 */
    fun decrypt(payload: String): String {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val ct = bytes.copyOfRange(12, bytes.size)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(128, iv))
        }.doFinal(ct).toString(Charsets.UTF_8)
    }

    private fun obtainKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}

/**
 * 自动登录凭据存储（需求 5：登录凭证永久生效，换设备失效）。
 *
 * 凭据 `username\npassword` 由 [CredentialCrypto] 加密落盘；离线/启动时
 * 解密凭据静默重登（需求 6 离线登录的凭据来源）。登出时清除。
 */
class CredentialStore(private val context: Context) {

    private object Keys {
        val CREDENTIAL = stringPreferencesKey("auto_login_credential")
    }

    /** 是否保存了自动登录凭据。 */
    val hasCredentialFlow: Flow<Boolean> =
        context.credentialDataStore.data.map { it[Keys.CREDENTIAL] != null }

    suspend fun save(username: String, password: String) {
        val payload = CredentialCrypto.encrypt("$username\n$password")
        context.credentialDataStore.edit { it[Keys.CREDENTIAL] = payload }
    }

    /** 还原凭据；无凭据或密钥不可用（换设备）返回 null。 */
    suspend fun restore(): Pair<String, String>? {
        val payload = context.credentialDataStore.data.first()[Keys.CREDENTIAL] ?: return null
        return try {
            val plain = CredentialCrypto.decrypt(payload)
            val sep = plain.indexOf('\n')
            if (sep <= 0) null else plain.substring(0, sep) to plain.substring(sep + 1)
        } catch (e: Exception) {
            // Keystore 密钥失效 = 换设备/清数据，按"无凭据"处理
            null
        }
    }

    suspend fun clear() {
        context.credentialDataStore.edit { it.remove(Keys.CREDENTIAL) }
    }
}
