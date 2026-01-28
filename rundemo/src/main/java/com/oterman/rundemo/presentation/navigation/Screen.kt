package com.oterman.rundemo.presentation.navigation

/**
 * 屏幕路由定义
 * 定义应用中所有的导航目的地
 */
sealed class Screen(val route: String) {
    /**
     * 登录页面
     */
    object Login : Screen("login")
    
    /**
     * 注册页面（暂未实现）
     */
    object Register : Screen("register")
    
    /**
     * 忘记密码页面（暂未实现）
     */
    object ForgotPassword : Screen("forgot_password")
    
    /**
     * 主页面（登录成功后跳转）
     */
    object Home : Screen("home")
    
    /**
     * 用户协议页面（暂未实现）
     */
    object UserTerms : Screen("user_terms")
    
    /**
     * 隐私政策页面（暂未实现）
     */
    object PrivacyPolicy : Screen("privacy_policy")
}

