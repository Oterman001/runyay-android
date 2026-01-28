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
}

