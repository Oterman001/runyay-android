package com.oterman.rundemo.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

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
     * 获取设备ID
     * 使用Android ID作为设备唯一标识
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
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
     */
    fun generateSign(
        params: Map<String, String> = emptyMap(),
        timestamp: String,
        appKey: String
    ): String {
        // 将参数按key排序
        val sortedParams = params.toSortedMap()
        
        // 构建签名字符串：appKey + timestamp + 排序后的参数
        val signString = buildString {
            append(appKey)
            append(timestamp)
            sortedParams.forEach { (key, value) ->
                append(key)
                append(value)
            }
        }
        
        // 使用SHA256加密
        return signString.sha256()
    }
}

