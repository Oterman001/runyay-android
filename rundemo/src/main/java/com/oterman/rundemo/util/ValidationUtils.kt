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
     * 验证验证码格式
     * 规则：6位数字
     */
    fun isValidSmsCode(smsCode: String): Boolean {
        if (smsCode.isEmpty()) return false
        val smsCodeRegex = "^\\d{6}$".toRegex()
        return smsCodeRegex.matches(smsCode)
    }
    
    /**
     * 验证昵称格式
     * 规则：1-20个字符，不能为空
     */
    fun isValidNickname(nickname: String): Boolean {
        return nickname.isNotEmpty() && nickname.length <= 20
    }

    /**
     * 验证昵称格式（严格模式，与iOS一致）
     * 规则：
     * - 长度: 1-20 字符
     * - 不能包含空格
     * - 只能含: 中文、英文、数字、下划线
     * - 正则: ^[\u4e00-\u9fa5a-zA-Z0-9_]+$
     */
    fun isValidNicknameStrict(nickname: String): Boolean {
        if (nickname.isEmpty() || nickname.length > 20) return false
        if (nickname.contains(" ")) return false
        val nicknameRegex = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$".toRegex()
        return nicknameRegex.matches(nickname)
    }

    /**
     * 获取严格模式昵称错误提示
     */
    fun getNicknameErrorStrict(nickname: String): String? {
        return when {
            nickname.isEmpty() -> "请输入昵称"
            nickname.length > 20 -> "昵称不能超过20个字符"
            nickname.contains(" ") -> "昵称不能包含空格"
            !isValidNicknameStrict(nickname) -> "昵称只能包含中文、英文、数字和下划线"
            else -> null
        }
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
    
    /**
     * 获取验证码错误提示
     */
    fun getSmsCodeError(smsCode: String): String? {
        if (smsCode.isEmpty()) return null
        return if (!isValidSmsCode(smsCode)) {
            "请输入6位验证码"
        } else {
            null
        }
    }
    
    /**
     * 获取昵称错误提示
     */
    fun getNicknameError(nickname: String): String? {
        if (nickname.isEmpty()) return "请输入昵称"
        return if (nickname.length > 20) {
            "昵称不能超过20个字符"
        } else {
            null
        }
    }
    
    /**
     * 获取确认密码错误提示
     */
    fun getConfirmPasswordError(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return null
        return if (password != confirmPassword) {
            "两次密码输入不一致"
        } else {
            null
        }
    }
}

