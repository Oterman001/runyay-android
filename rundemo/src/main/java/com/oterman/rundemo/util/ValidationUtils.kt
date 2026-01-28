package com.oterman.rundemo.util

/**
 * 验证工具类
 */
object ValidationUtils {
    
    /**
     * 验证手机号格式
     * 规则：以1开头，第二位是3-9，共11位数字
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) return false
        val phoneRegex = "^1[3-9]\\d{9}$".toRegex()
        return phoneRegex.matches(phoneNumber)
    }
    
    /**
     * 验证密码格式
     * 规则：至少6位字符
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * 获取手机号错误提示
     */
    fun getPhoneNumberError(phoneNumber: String): String? {
        if (phoneNumber.isEmpty()) return null
        return if (!isValidPhoneNumber(phoneNumber)) {
            "请输入正确的手机号"
        } else {
            null
        }
    }
    
    /**
     * 获取密码错误提示
     */
    fun getPasswordError(password: String): String? {
        if (password.isEmpty()) return null
        return if (!isValidPassword(password)) {
            "密码长度不能少于6位"
        } else {
            null
        }
    }
}

