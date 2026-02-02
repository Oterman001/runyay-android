package com.oterman.rundemo.presentation.navigation

/**
 * 屏幕路由定义
 * 定义应用中所有的导航目的地
 */
sealed class Screen(val route: String) {
    /**
     * 欢迎页面
     */
    object Welcome : Screen("welcome")

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
     * 用户信息页面
     * 用于查看和编辑个人资料
     */
    object UserProfile : Screen("user_profile")
    
    /**
     * 用户协议页面（暂未实现）
     */
    object UserTerms : Screen("user_terms")
    
    /**
     * 隐私政策页面（暂未实现）
     */
    object PrivacyPolicy : Screen("privacy_policy")
    
    /**
     * 跑步详情页（用户友好版本）
     * 展示地图、数据概览和分段数据
     */
    object RunDetail : Screen("run_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "run_detail/$workoutId"
    }

    /**
     * 跑步记录调试详情页
     * 用于展示跑步记录的完整数据信息
     */
    object RunDetailDebug : Screen("run_detail_debug/{workoutId}") {
        fun createRoute(workoutId: String) = "run_detail_debug/$workoutId"
    }
    
    /**
     * 数据源管理页面
     * 用于管理佳明、高驰等第三方数据源的授权和同步
     */
    object DataSourceManage : Screen("data_source_manage")
    
    /**
     * 数据源详情页面
     * 用于查看和操作单个数据源（授权/解绑/同步）
     */
    object DataSourceDetail : Screen("data_source_detail/{platformCode}") {
        fun createRoute(platformCode: String) = "data_source_detail/$platformCode"
    }
    
    /**
     * OAuth授权WebView页面
     * 用于加载第三方平台的OAuth授权页面
     */
    object OAuthWebView : Screen("oauth_webview/{platformCode}?authUrl={authUrl}") {
        fun createRoute(platformCode: String, authUrl: String): String {
            val encodedUrl = java.net.URLEncoder.encode(authUrl, "UTF-8")
            return "oauth_webview/$platformCode?authUrl=$encodedUrl"
        }
    }

    /**
     * 数据源调试页面
     * 用于查看数据源的同步状态和记录
     */
    object DataSourceDebug : Screen("data_source_debug/{platformCode}") {
        fun createRoute(platformCode: String) = "data_source_debug/$platformCode"
    }

    /**
     * 数据源记录列表页面
     * 用于查看和管理指定数据源的所有记录
     */
    object DataSourceRecordList : Screen("data_source_record_list/{platformCode}") {
        fun createRoute(platformCode: String) = "data_source_record_list/$platformCode"
    }
}

