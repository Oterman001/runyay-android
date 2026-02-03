package com.oterman.rundemo.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 密钥常量类
 * 对应iOS的SecretConstants，使用混淆算法保护密钥
 */
object SecretConstants {
    // 混淆后的字节数组（来自iOS keyArr）
    private val KEY_ARR = byteArrayOf(
        128.toByte(), 123.toByte(), 129.toByte(), 107.toByte(),
        56.toByte(), 40.toByte(), 56.toByte(), 47.toByte(),
        130.toByte(), 56.toByte(), 66.toByte(), 47.toByte(),
        128.toByte(), 51.toByte(), 43.toByte(), 39.toByte()
    )
    
    // 密钥（来自iOS salut）
    private const val SALUT = "com.yaya.run.ios"
    
    // 盐值
    private const val SALT: Byte = 37
    
    // 懒加载解密后的密钥
    val mySpecialID: String by lazy { reveal() }
    
    /**
     * 解密混淆后的密钥
     * 对应iOS的SecretConstants.reveal()方法
     */
    private fun reveal(): String {
        val keyBytes = SALUT.toByteArray(Charsets.UTF_8)
        val revealedBytes = ByteArray(KEY_ARR.size)
        
        for (i in KEY_ARR.indices) {
            // 1. 减去盐值（使用无符号运算）
            val saltRemovedByte = (KEY_ARR[i].toInt() and 0xFF) - (SALT.toInt() and 0xFF)
            
            // 2. 使用密钥字节进行XOR操作
            val keyByte = keyBytes[i % keyBytes.size]
            val originalByte = (saltRemovedByte xor keyByte.toInt()).toByte()
            
            revealedBytes[i] = originalByte
        }
        
        return String(revealedBytes, Charsets.UTF_8)
    }
}

/**
 * 安全工具类
 * 提供加密和设备标识相关功能
 */
object SecurityUtils {
    
    /**
     * MD5加密
     * 将字符串进行MD5加密，返回32位小写字符串
     */
    fun String.md5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * SHA256加密
     * 将字符串进行SHA256加密，返回64位小写字符串
     */
    fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * HMAC-SHA256签名
     * 对应iOS的SignatureUtil.sign方法
     * 
     * @param secret 密钥
     * @param content 待签名内容
     * @return HMAC-SHA256签名的十六进制字符串
     */
    fun hmacSha256(secret: String, content: String): String {
        // 1. 先对content进行SHA-256哈希
        val hashedContent = content.sha256()
        
        // 2. 使用secret作为密钥，对hashedContent进行HMAC-SHA256
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        
        val hmacBytes = mac.doFinal(hashedContent.toByteArray(Charsets.UTF_8))
        
        // 3. 转换为十六进制字符串
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
    
    private const val PREFS_NAME = "security_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    /**
     * 获取设备ID
     * 优先使用Android ID，如果获取不到则生成UUID并持久化存储
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        // 1. 尝试获取Android ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            // 9774d56d682e549c 是某些设备的默认值，不可靠
            return androidId
        }

        // 2. 尝试从本地存储获取已保存的UUID
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDeviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (!savedDeviceId.isNullOrBlank()) {
            return savedDeviceId
        }

        // 3. 生成新的UUID并持久化存储
        val newDeviceId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newDeviceId).apply()
        return newDeviceId
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    fun getTimestamp(): String {
        return System.currentTimeMillis().toString()
    }
    
    /**
     * 生成签名
     * 对应iOS的NetworkUtils.generateSign方法
     * 使用HMAC-SHA256算法，与iOS保持一致
     * 
     * @param params 请求参数（当前未使用，保留以备后续扩展）
     * @param timestamp 时间戳
     * @param appKey AppKey
     * @return HMAC-SHA256签名字符串
     */
    fun generateSign(
        params: Map<String, String> = emptyMap(),
        timestamp: String,
        appKey: String
    ): String {
        // 构建待签名内容：appKey + timestamp
        // 对应iOS: SignatureUtil.sign(secret: SecretConstants.mySpecialID, content: "\(appKey)\(timestamp)")
        val content = "$appKey$timestamp"
        
        // 使用解密后的密钥进行HMAC-SHA256签名
        return hmacSha256(secret = SecretConstants.mySpecialID, content = content)
    }
}

