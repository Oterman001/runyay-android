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
     * 绑定引导页面
     * 登录/注册成功后引导用户绑定运动手表平台
     */
    object BindingGuide : Screen("binding_guide")

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
     * 手动导入落地页
     * 统一的 FIT 文件导入入口，支持多选批量导入，并展示已导入记录列表（支持删除）
     */
    object ManualImport : Screen("manual_import")
    
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

    /**
     * 平台记录列表页面（用户版）
     * 查看指定平台导入的所有跑步记录
     */
    object PlatformRecordList : Screen("platform_record_list/{platformCode}") {
        fun createRoute(platformCode: String) = "platform_record_list/$platformCode"
    }

    /**
     * 跑步目标设置页面
     * 用于设置年度/月度跑步目标
     */
    object RunGoalSet : Screen("run_goal_set")

    /**
     * 心率区间设置页面
     * 用于手动配置最大心率、静息心率及查看7区间结果
     */
    object HearRateZoneSet : Screen("hear_rate_zone_set")

    /**
     * 生理参数初始化引导页面
     * 新用户注册完成或存量用户首次登录时引导设置生理参数
     */
    object PhysioSetup : Screen("physio_setup?nextDest={nextDest}") {
        fun createRoute(nextDest: String = "home") = "physio_setup?nextDest=$nextDest"
    }

    /**
     * 跑步统计页面
     * 用于展示年/月/周/总统计数据
     */
    object RunStatistics : Screen("run_statistics/{tab}") {
        fun createRoute(tab: String = "week") = "run_statistics/$tab"
    }

    /**
     * 调试页面
     * 用于开发调试功能（仅Debug版本可见）
     */
    object Debug : Screen("debug")

    /**
     * 数据同步状态页面
     * 用于展示数据同步进度和导入记录
     */
    object DataSyncStatus : Screen("data_sync_status")

    /**
     * 所有跑步记录管理页面（调试用）
     * 支持按数据源筛选、多选删除、全选删除
     */
    object AllRunRecords : Screen("all_run_records")

    /**
     * 联系我们页面
     * 提供微信、小红书、发送日志等联系方式
     */
    object ContactUs : Screen("contact_us")

    /**
     * 微信公众号二维码页面
     * 展示公众号二维码，支持一键复制并打开微信
     */
    object WeChatQrCode : Screen("wechat_qr_code")
}

