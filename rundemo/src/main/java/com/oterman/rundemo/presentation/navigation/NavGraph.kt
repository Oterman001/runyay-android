package com.oterman.rundemo.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.oterman.rundemo.RunDetailActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.ForgotPasswordScreen
import com.oterman.rundemo.presentation.feature.auth.login.LoginScreen
import com.oterman.rundemo.presentation.feature.auth.register.RegisterScreen
import com.oterman.rundemo.presentation.feature.datasource.DataSourceDetailScreen
import com.oterman.rundemo.presentation.feature.datasource.DataSourceDetailViewModel
import com.oterman.rundemo.presentation.feature.datasource.DataSourceDetailViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.DataSourceManageScreen
import com.oterman.rundemo.presentation.feature.datasource.DataSourceManageViewModel
import com.oterman.rundemo.presentation.feature.datasource.DataSourceManageViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.OAuthWebViewScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugViewModel
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListViewModel
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListViewModelFactory
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideScreen
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideViewModel
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideViewModelFactory
import com.oterman.rundemo.presentation.feature.home.HomeScreen
import com.oterman.rundemo.presentation.feature.home.HomeViewModel
import com.oterman.rundemo.presentation.feature.home.HomeViewModelFactory
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailDebugScreen
import com.oterman.rundemo.presentation.feature.settings.goal.RunGoalSetPage
import com.oterman.rundemo.presentation.feature.statistics.RunStatisticTab
import com.oterman.rundemo.presentation.feature.statistics.RunStatisticsScreen
import com.oterman.rundemo.presentation.feature.userprofile.UserProfileScreen
import com.oterman.rundemo.presentation.feature.debug.DebugScreen
import com.oterman.rundemo.presentation.feature.debug.allrecords.AllRunRecordsDebugScreen
import com.oterman.rundemo.presentation.feature.syncstatus.DataSyncStatusScreen
import com.oterman.rundemo.presentation.feature.welcome.WelcomeScreen
import com.oterman.rundemo.ui.theme.ThemeMode

/**
 * 应用导航图
 * 定义应用的导航结构和页面跳转逻辑
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 欢迎页面
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        // 登录页面
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginSuccess = {
                    // 登录成功后导航到绑定引导页
                    navController.navigate(Screen.BindingGuide.route) {
                        // 清除欢迎页面和登录页面，防止返回
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        
        // 绑定引导页面（登录/注册成功后）
        composable(Screen.BindingGuide.route) {
            val context = LocalContext.current
            val viewModel: BindingGuideViewModel = viewModel(
                factory = BindingGuideViewModelFactory(context)
            )
            BindingGuideScreen(
                viewModel = viewModel,
                onSkip = {
                    viewModel.markGuideCompleted()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.BindingGuide.route) { inclusive = true }
                    }
                },
                onComplete = {
                    viewModel.markGuideCompleted()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.BindingGuide.route) { inclusive = true }
                    }
                },
                onNavigateToDetail = { platform ->
                    navController.navigate(Screen.DataSourceDetail.createRoute(platform.code))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.BindingGuide.route) { inclusive = true }
                    }
                }
            )
        }

        // 主页面（登录成功后的页面）
        composable(Screen.Home.route) { backStackEntry ->
            val context = LocalContext.current
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(context)
            )

            // 监听从UserProfileScreen返回时的刷新信号
            LaunchedEffect(Unit) {
                backStackEntry.savedStateHandle.getStateFlow("profile_updated", false)
                    .collect { updated ->
                        if (updated) {
                            homeViewModel.refreshProfileData()
                            backStackEntry.savedStateHandle["profile_updated"] = false
                        }
                    }
            }

            HomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route)
                },
                onNavigateToRunDetail = { workoutId ->
                    // 使用独立Activity以避免MapView销毁阻塞返回动画
                    val intent = RunDetailActivity.createIntent(context, workoutId)
                    context.startActivity(intent)
                },
                onNavigateToRunDetailDebug = { workoutId ->
                    navController.navigate(Screen.RunDetailDebug.createRoute(workoutId))
                },
                onNavigateToDataSourceManage = {
                    navController.navigate(Screen.DataSourceManage.route)
                },
                onNavigateToUserProfile = {
                    navController.navigate(Screen.UserProfile.route)
                },
                onNavigateToRunGoalSet = {
                    navController.navigate(Screen.RunGoalSet.route)
                },
                onNavigateToRunStatistics = { tab ->
                    navController.navigate(Screen.RunStatistics.createRoute(tab))
                },
                onNavigateToDebug = {
                    navController.navigate(Screen.Debug.route)
                },
                onNavigateToSyncStatus = {
                    navController.navigate(Screen.DataSyncStatus.route)
                },
                onThemeModeChanged = onThemeModeChanged,
                viewModel = homeViewModel
            )
        }

        // 用户信息页面
        composable(Screen.UserProfile.route) {
            // 记录HomeScreen的BackStackEntry，用于在离开时通知刷新
            val homeEntry = remember { navController.previousBackStackEntry }

            // 无论通过返回按钮还是手势返回，离开时都通知HomeScreen刷新
            DisposableEffect(Unit) {
                onDispose {
                    homeEntry?.savedStateHandle?.set("profile_updated", true)
                }
            }

            UserProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    // 退出登录或注销后导航到登录页
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 注册页面
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // 注册成功后导航到绑定引导页
                    navController.navigate(Screen.BindingGuide.route) {
                        // 清除欢迎页面和注册页面，防止返回
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { phoneNumber ->
                    // 导航到登录页面，可以携带手机号
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToUserTerms = {
                    navController.navigate(Screen.UserTerms.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }
        
        // 忘记密码页面
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = { phoneNumber ->
                    // 重置成功后导航到登录页面，携带手机号
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // 用户不存在时导航到注册页面
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 用户协议页面（占位符）
        composable(Screen.UserTerms.route) {
            PlaceholderScreen(title = "用户协议")
        }
        
        // 隐私政策页面（占位符）
        composable(Screen.PrivacyPolicy.route) {
            PlaceholderScreen(title = "隐私政策")
        }
        
        // 跑步详情页（用户友好版本）已迁移到独立的 RunDetailActivity
        // 使用 startActivity 而非 Compose Navigation，以避免 MapView 销毁阻塞返回动画

        // 跑步记录调试详情页
        composable(
            route = Screen.RunDetailDebug.route,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            RunDetailDebugScreen(
                workoutId = workoutId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // 数据源管理页面
        composable(Screen.DataSourceManage.route) {
            val context = LocalContext.current
            val viewModel: DataSourceManageViewModel = viewModel(
                factory = DataSourceManageViewModelFactory(context)
            )
            DataSourceManageScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { platform ->
                    navController.navigate(Screen.DataSourceDetail.createRoute(platform.code))
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToHkDebug = {
                    navController.navigate(
                        Screen.DataSourceDebug.createRoute(DataSourcePlatform.APPLE_HEALTH.code)
                    )
                }
            )
        }
        
        // 数据源详情页面
        composable(
            route = Screen.DataSourceDetail.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            val viewModel: DataSourceDetailViewModel = viewModel(
                factory = DataSourceDetailViewModelFactory(context, platform)
            )
            DataSourceDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenOAuthWebView = { authUrl ->
                    navController.navigate(Screen.OAuthWebView.createRoute(platformCode, authUrl))
                },
                onNavigateToDebug = {
                    navController.navigate(Screen.DataSourceDebug.createRoute(platformCode))
                }
            )
        }
        
        // OAuth授权WebView页面
        composable(
            route = Screen.OAuthWebView.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType },
                navArgument("authUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val authUrlEncoded = backStackEntry.arguments?.getString("authUrl") ?: return@composable
            val authUrl = java.net.URLDecoder.decode(authUrlEncoded, "UTF-8")
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            
            // 获取父级DataSourceDetail页面的ViewModel
            val parentEntry = navController.getBackStackEntry(Screen.DataSourceDetail.createRoute(platformCode))
            val detailViewModel: DataSourceDetailViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = DataSourceDetailViewModelFactory(context, platform)
            )
            
            OAuthWebViewScreen(
                authUrl = authUrl,
                platform = platform,
                onAuthCallback = { params ->
                    // 直接调用ViewModel处理OAuth回调，发送请求到服务器
                    detailViewModel.handleOAuthCallback(params)
                    navController.popBackStack()
                },
                onDismiss = {
                    detailViewModel.dismissOAuthWebView()
                    navController.popBackStack()
                }
            )
        }

        // 数据源调试页面
        composable(
            route = Screen.DataSourceDebug.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            val viewModel: DataSourceDebugViewModel = viewModel(
                factory = DataSourceDebugViewModelFactory(context, platform)
            )
            DataSourceDebugScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecordList = {
                    navController.navigate(Screen.DataSourceRecordList.createRoute(platformCode))
                }
            )
        }

        // 数据源记录列表页面
        composable(
            route = Screen.DataSourceRecordList.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            val viewModel: DataSourceRecordListViewModel = viewModel(
                factory = DataSourceRecordListViewModelFactory(context, platform)
            )
            DataSourceRecordListScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { workoutId ->
                    // 使用独立Activity以避免MapView销毁阻塞返回动画
                    val intent = RunDetailActivity.createIntent(context, workoutId)
                    context.startActivity(intent)
                },
                onNavigateToDebugDetail = { workoutId ->
                    navController.navigate(Screen.RunDetailDebug.createRoute(workoutId))
                }
            )
        }

        // 跑步目标设置页面
        composable(Screen.RunGoalSet.route) {
            RunGoalSetPage(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 跑步统计页面
        composable(
            route = Screen.RunStatistics.route,
            arguments = listOf(
                navArgument("tab") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tabName = backStackEntry.arguments?.getString("tab") ?: "week"
            val context = LocalContext.current
            RunStatisticsScreen(
                initialTab = RunStatisticTab.fromName(tabName),
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRunDetail = { workoutId ->
                    // 使用独立Activity以避免MapView销毁阻塞返回动画
                    val intent = RunDetailActivity.createIntent(context, workoutId)
                    context.startActivity(intent)
                }
            )
        }

        // 调试页面
        composable(Screen.Debug.route) {
            DebugScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAllRunRecords = {
                    navController.navigate(Screen.AllRunRecords.route)
                },
                onNavigateToDataSourceDebug = { platformCode ->
                    navController.navigate(Screen.DataSourceDebug.createRoute(platformCode))
                }
            )
        }

        // 所有跑步记录管理页面
        composable(Screen.AllRunRecords.route) {
            AllRunRecordsDebugScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 数据同步状态页面
        composable(Screen.DataSyncStatus.route) {
            DataSyncStatusScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 占位符页面
 * 用于暂未实现的页面
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title\n（待实现）",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

