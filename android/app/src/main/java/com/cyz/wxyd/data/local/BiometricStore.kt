package com.cyz.wxyd.data.local

import android.content.Context
import android.os.Build
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

private val Context.biometricDataStore by preferencesDataStore(name = "wxyd_biometric")

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "wxyd_biometric_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/**
 * 指纹登录凭据的加密/解密（AndroidKeyStore AES-GCM）。
 *
 * 密钥开启 `setUserAuthenticationRequired` + 30 秒有效窗口
 * （`setUserAuthenticationValidityDurationSeconds`）：解密操作必须在最近一次
 * 指纹/锁屏验证后的 30 秒内完成。这样同时兼容 Class 3（强）与 Class 2（弱）
 * 指纹传感器——后者不支持 per-operation CryptoObject，但支持时限窗口模式；
 * 凭据密文仍只在本机可解（无私钥即不可还原），密钥不出安全硬件。
 */
object BiometricCrypto {

    /** 加密为 `Base64(iv ++ ciphertext)` 单串，便于整体存 DataStore（须在验证窗口内）。 */
    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, obtainKey())
        }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ct, Base64.NO_WRAP)
    }

    /** 解密 [payload]（[encrypt] 的产物）；指纹校验通过后的窗口内调用。 */
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
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(30)
            .apply {
                // 指纹录入增删导致密钥失效的体验很差（用户得重新设密码），
                // 与"指纹仅作为解锁凭据的钥匙"的定位一致：指纹集变化不吊销密钥。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setInvalidatedByBiometricEnrollment(false)
                }
            }
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}

/**
 * 指纹登录本地存储（DataStore + [BiometricCrypto]）。
 *
 * 持久化四项：
 * - 是否启用指纹登录（enabled）
 * - 启用时间保存的加密凭据（credential = Base64(iv ++ AES-GCM(username\ password))）
 * - 启动引导是否已展示过（guideSeen，跳过后不再每次登录都弹）
 *
 * 约定：凭据里 username 与 password 以 `\n` 分隔（用户名不含换行）。
 * 加密/解密须在 BiometricPrompt 成功后的 30 秒时限窗口内完成。
 */
class BiometricStore(private val context: Context) {

    private object Keys {
        val ENABLED = stringPreferencesKey("biometric_enabled")
        val CREDENTIAL = stringPreferencesKey("biometric_credential")
        val GUIDE_SEEN = stringPreferencesKey("biometric_guide_seen")
    }

    /** 是否已启用指纹登录。 */
    val enabledFlow: Flow<Boolean> =
        context.biometricDataStore.data.map { it[Keys.ENABLED] == "1" }

    /** 登录后引导弹窗是否已展示过（跳过也算）。 */
    val guideSeenFlow: Flow<Boolean> =
        context.biometricDataStore.data.map { it[Keys.GUIDE_SEEN] == "1" }

    /** 启用指纹登录：加密保存凭据（须在指纹验证后的 30 秒窗口内调用）。 */
    suspend fun enable(username: String, password: String) {
        val payload = BiometricCrypto.encrypt("$username\n$password")
        context.biometricDataStore.edit {
            it[Keys.ENABLED] = "1"
            it[Keys.CREDENTIAL] = payload
        }
    }

    /** 关闭指纹登录并清除本机保存的凭据。 */
    suspend fun disable() {
        context.biometricDataStore.edit {
            it.remove(Keys.ENABLED)
            it.remove(Keys.CREDENTIAL)
        }
    }

    /** 引导弹窗展示过即标记（含用户选择"暂不"）。 */
    suspend fun markGuideSeen() {
        context.biometricDataStore.edit { it[Keys.GUIDE_SEEN] = "1" }
    }

    /** 启用时间保存的密文（Base64(iv ++ ct)，供取 IV 组装 CryptoObject）。 */
    val credentialPayloadFlow: Flow<String?> =
        context.biometricDataStore.data.map { it[Keys.CREDENTIAL] }

    /** 一次性读取密文（指纹登录入口组装 CryptoObject 时用）。 */
    suspend fun credentialPayload(): String? =
        context.biometricDataStore.data.first()[Keys.CREDENTIAL]

    /**
     * 指纹校验成功后（30 秒窗口内）还原凭据。[payload] 为 [credentialPayload] 的密文；
     * 密钥被系统吊销（如锁屏凭据被清）会抛出异常，由上层按"需重新启用"提示。
     */
    suspend fun restoreCredential(payload: String): Pair<String, String>? {
        val plain = BiometricCrypto.decrypt(payload)
        val sep = plain.indexOf('\n')
        if (sep <= 0) return null
        return plain.substring(0, sep) to plain.substring(sep + 1)
    }
}
