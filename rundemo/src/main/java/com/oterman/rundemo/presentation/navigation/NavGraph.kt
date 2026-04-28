package com.oterman.rundemo.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.oterman.rundemo.MainActivity
import com.oterman.rundemo.RunDetailActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oterman.rundemo.domain.model.DataSourcePlatform
import com.oterman.rundemo.presentation.feature.contactus.ContactUsScreen
import com.oterman.rundemo.presentation.feature.contactus.WeChatQrCodeScreen
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
import com.oterman.rundemo.presentation.feature.datasource.records.PlatformRecordListScreen
import com.oterman.rundemo.presentation.feature.datasource.records.PlatformRecordListViewModel
import com.oterman.rundemo.presentation.feature.datasource.records.PlatformRecordListViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.manualimport.ManualImportScreen
import com.oterman.rundemo.presentation.feature.datasource.manualimport.ManualImportViewModel
import com.oterman.rundemo.presentation.feature.datasource.manualimport.ManualImportViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugViewModel
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceDebugViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListViewModel
import com.oterman.rundemo.presentation.feature.datasource.debug.DataSourceRecordListViewModelFactory
import com.oterman.rundemo.presentation.feature.datasource.debug.ServerActivityListScreen
import com.oterman.rundemo.presentation.feature.datasource.debug.ServerActivityListViewModel
import com.oterman.rundemo.presentation.feature.datasource.debug.ServerActivityListViewModelFactory
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideScreen
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideViewModel
import com.oterman.rundemo.presentation.feature.onboarding.BindingGuideViewModelFactory
import com.oterman.rundemo.presentation.feature.onboarding.physio.PhysioSetupScreen
import com.oterman.rundemo.presentation.feature.onboarding.physio.PhysioSetupViewModel
import com.oterman.rundemo.presentation.feature.onboarding.physio.PhysioSetupViewModelFactory
import com.oterman.rundemo.presentation.feature.home.HomeScreen
import com.oterman.rundemo.presentation.feature.home.HomeViewModel
import com.oterman.rundemo.presentation.feature.home.HomeViewModelFactory
import com.oterman.rundemo.presentation.feature.rundetail.RunDetailDebugScreen
import com.oterman.rundemo.presentation.feature.settings.goal.RunGoalSetPage
import com.oterman.rundemo.presentation.feature.settings.heartrate.HearRateZoneScreen
import com.oterman.rundemo.presentation.feature.statistics.RunStatisticTab
import com.oterman.rundemo.presentation.feature.statistics.RunStatisticsScreen
import com.oterman.rundemo.presentation.feature.userprofile.UserProfileScreen
import com.oterman.rundemo.presentation.feature.debug.DebugScreen
import com.oterman.rundemo.presentation.feature.debug.vdot.VdotDebugScreen
import com.oterman.rundemo.presentation.feature.debug.allrecords.AllRunRecordsDebugScreen
import com.oterman.rundemo.BuildConfig
import com.oterman.rundemo.presentation.feature.debug.synccontrol.SyncControlScreen
import com.oterman.rundemo.presentation.feature.debug.synccontrol.SyncControlViewModel
import com.oterman.rundemo.presentation.feature.debug.synccontrol.SyncControlViewModelFactory
import com.oterman.rundemo.presentation.feature.syncstatus.DataSyncStatusScreen
import com.oterman.rundemo.presentation.feature.legal.PrivacyConsentScreen
import com.oterman.rundemo.presentation.feature.legal.PrivacyPolicyScreen
import com.oterman.rundemo.presentation.feature.legal.UserTermsScreen
import com.oterman.rundemo.presentation.feature.runningshoes.RunningShoesManagementScreen
import com.oterman.rundemo.presentation.feature.runningshoes.detail.RunningShoeDetailScreen
import com.oterman.rundemo.presentation.feature.runningshoes.addedit.AddEditRunningShoeScreen
import com.oterman.rundemo.presentation.feature.runningshoes.linkedrecords.LinkedRunRecordsListScreen
import com.oterman.rundemo.presentation.feature.vdotdetail.VdotDetailScreen
import com.oterman.rundemo.presentation.feature.welcome.WelcomeScreen
import com.oterman.rundemo.ui.theme.ThemeMode
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.data.local.database.RunDatabase
import com.oterman.rundemo.data.repository.RunDataRepositoryImpl
import com.oterman.rundemo.data.repository.TokenRefreshManager
import com.oterman.rundemo.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current

    // 全局监听 Token 作废事件：任何页面收到 0022 且 refreshToken 也失败时，
    // 清空返回栈并跳转到登录页，避免用户停留在无效会话中
    LaunchedEffect(Unit) {
        TokenRefreshManager.getInstance(context).tokenExpiredEvent.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        // 隐私政策同意页面（首次启动）
        composable(Screen.PrivacyConsent.route) {
            PrivacyConsentScreen(
                onAgreed = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.PrivacyConsent.route) { inclusive = true }
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
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginSuccess = {
                    scope.launch {
                        val prefsManager = PreferencesManager(context)

                        // 确保 RunDataRepository 的 userId 已设置（SyncControl 等页面需要）
                        val database = RunDatabase.getInstance(context)
                        val runDataRepo = RunDataRepositoryImpl.getInstance(database)
                        prefsManager.getUserId()?.let { runDataRepo.setCurrentUserId(it) }

                        val userRepo = UserRepository(context)
                        val result = userRepo.queryBasicInfo()
                        val serverInfo = result.getOrNull()
                        if (serverInfo != null) {
                            // 服务端有数据 → 同步到本地并标记已完成
                            val cur = prefsManager.getHearRateZoneSettings()
                            prefsManager.saveHearRateZoneSettings(cur.copy(
                                isMale = serverInfo.gender != "F",
                                birthdayMillis = serverInfo.birthDate?.let {
                                    SimpleDateFormat("yyyyMMdd", Locale.US).parse(it)?.time ?: cur.birthdayMillis
                                } ?: cur.birthdayMillis,
                                maxHeartRate = serverInfo.maxHeartRate ?: cur.maxHeartRate,
                                restingHeartRate = serverInfo.manualRestingHeartRate ?: cur.restingHeartRate
                            ))
                            prefsManager.markPhysioSetupCompleted()
                            navController.navigate(Screen.BindingGuide.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        } else {
                            // 服务端无数据 → 引导设置
                            navController.navigate(Screen.PhysioSetup.createRoute("binding_guide")) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToContactUs = {
                    navController.navigate(Screen.ContactUs.route)
                },
                onNavigateToUserTerms = {
                    navController.navigate(Screen.UserTerms.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        // 绑定引导页面（登录/注册成功后）
        composable(Screen.BindingGuide.route) {
            val context = LocalContext.current
            val viewModel: BindingGuideViewModel = viewModel(
                factory = BindingGuideViewModelFactory(context)
            )
            val navigateAfterBinding = {
                viewModel.markGuideCompleted()
                if (BuildConfig.DEBUG) {
                    navController.navigate(Screen.SyncControl.createRoute(showHome = true)) {
                        popUpTo(Screen.BindingGuide.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.BindingGuide.route) { inclusive = true }
                    }
                }
            }
            BindingGuideScreen(
                viewModel = viewModel,
                onSkip = navigateAfterBinding,
                onComplete = navigateAfterBinding,
                onNavigateToDetail = { platform ->
                    navController.navigate(Screen.DataSourceDetail.createRoute(platform.code))
                },
                onNavigateToHome = navigateAfterBinding
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

            // 未登录时从微信打开 .fit 文件，登录后进入首页时检查是否有待导入 URI。
            // StateFlow 保留了登录前存储的值，此处一次性消费并跳转到 ManualImport。
            LaunchedEffect(Unit) {
                if (MainActivity.pendingShareUris.value.isNotEmpty()) {
                    navController.navigate(Screen.ManualImport.route) {
                        launchSingleTop = true
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
                onNavigateToHearRateZoneSet = {
                    navController.navigate(Screen.HearRateZoneSet.route)
                },
                onNavigateToRunStatistics = { tab ->
                    navController.navigate(Screen.RunStatistics.createRoute(tab))
                },
                onNavigateToDebug = {
                    navController.navigate(Screen.Debug.route)
                },
                onNavigateToContactUs = {
                    navController.navigate(Screen.ContactUs.route)
                },
                onNavigateToSyncStatus = {
                    navController.navigate(Screen.DataSyncStatus.route)
                },
                onNavigateToVdotDetail = {
//                    navController.navigate(Screen.VdotDetail.route)
                },
                onNavigateToRunningShoes = {
                    navController.navigate(Screen.RunningShoes.route)
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
                    // 注册成功后先引导设置生理参数，再进入绑定引导
                    navController.navigate(Screen.PhysioSetup.createRoute("binding_guide")) {
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
                },
                onNavigateToContactUs = {
                    navController.navigate(Screen.ContactUs.route)
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
        
        // 用户协议页面
        composable(Screen.UserTerms.route) {
            UserTermsScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        // 隐私政策页面
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
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
                onNavigateToContactUs = {
                    navController.navigate(Screen.ContactUs.route)
                },
                onNavigateToManualImport = {
                    navController.navigate(Screen.ManualImport.route)
                },
                onNavigateToHkDebug = {
                    navController.navigate(
                        Screen.DataSourceDebug.createRoute(DataSourcePlatform.APPLE_HEALTH.code)
                    )
                }
            )
        }

        // 手动导入落地页
        composable(Screen.ManualImport.route) { backStackEntry ->
            val context = LocalContext.current
            val manualImportViewModel: ManualImportViewModel = viewModel(
                factory = ManualImportViewModelFactory(context)
            )
            ManualImportScreen(
                viewModel = manualImportViewModel,
                navBackStackEntry = backStackEntry,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecordList = {
                    navController.navigate(
                        Screen.PlatformRecordList.createRoute(DataSourcePlatform.MANUAL.code)
                    )
                },
                onNavigateToRunDetail = { workoutId ->
                    context.startActivity(RunDetailActivity.createIntent(context, workoutId))
                },
                onNavigateToDebug = {
                    navController.navigate(Screen.DataSourceDebug.createRoute("MANUAL"))
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
                },
                onNavigateToRecordList = {
                    navController.navigate(Screen.PlatformRecordList.createRoute(platformCode))
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
                },
                onNavigateToServerActivityList = {
                    navController.navigate(Screen.ServerActivityList.createRoute(platformCode))
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

        // 服务端活动列表页面
        composable(
            route = Screen.ServerActivityList.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            val viewModel: ServerActivityListViewModel = viewModel(
                factory = ServerActivityListViewModelFactory(context, platform)
            )
            ServerActivityListScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 平台记录列表页面（用户版）
        composable(
            route = Screen.PlatformRecordList.route,
            arguments = listOf(
                navArgument("platformCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val platformCode = backStackEntry.arguments?.getString("platformCode") ?: return@composable
            val platform = DataSourcePlatform.fromCode(platformCode) ?: return@composable
            val context = LocalContext.current
            val viewModel: PlatformRecordListViewModel = viewModel(
                factory = PlatformRecordListViewModelFactory(context, platform)
            )
            PlatformRecordListScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRunDetail = { workoutId ->
                    val intent = RunDetailActivity.createIntent(context, workoutId)
                    context.startActivity(intent)
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

        // 生理参数初始化引导页面
        composable(
            route = Screen.PhysioSetup.route,
            arguments = listOf(navArgument("nextDest") {
                type = NavType.StringType
                defaultValue = "home"
            })
        ) { backStackEntry ->
            val nextDest = backStackEntry.arguments?.getString("nextDest") ?: "home"
            val context = LocalContext.current
            val physioViewModel = viewModel<PhysioSetupViewModel>(
                factory = PhysioSetupViewModelFactory(context)
            )
            PhysioSetupScreen(
                viewModel = physioViewModel,
                onComplete = {
                    if (nextDest == "binding_guide") {
                        navController.navigate(Screen.BindingGuide.route) {
                            popUpTo(Screen.PhysioSetup.route) { inclusive = true }
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            navController.navigate(Screen.SyncControl.createRoute(showHome = true)) {
                                popUpTo(Screen.PhysioSetup.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.PhysioSetup.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // 心率区间设置页面
        composable(Screen.HearRateZoneSet.route) {
            HearRateZoneScreen(
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
                },
                onNavigateToSyncControl = {
                    navController.navigate(Screen.SyncControl.route)
                },
                onNavigateToVdotDebug = {
                    navController.navigate(Screen.VdotDebug.route)
                }
            )
        }

        // VDOT计算调试页面
        composable(Screen.VdotDebug.route) {
            VdotDebugScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 同步控制调试页面
        composable(
            route = Screen.SyncControl.route,
            arguments = listOf(navArgument("showHome") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val showHome = backStackEntry.arguments?.getBoolean("showHome") ?: false
            val context = LocalContext.current
            val viewModel: SyncControlViewModel = viewModel(
                factory = SyncControlViewModelFactory(context)
            )
            SyncControlScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = if (showHome) {
                    {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SyncControl.route) { inclusive = true }
                        }
                    }
                } else null
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

        // 联系我们页面
        composable(Screen.ContactUs.route) {
            ContactUsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToWeChat = {
                    navController.navigate(Screen.WeChatQrCode.route)
                },
                onNavigateToUserTerms = {
                    navController.navigate(Screen.UserTerms.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        // 微信公众号二维码页面
        composable(Screen.WeChatQrCode.route) {
            WeChatQrCodeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // VDOT跑力详情页面
        composable(Screen.VdotDetail.route) {
            VdotDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 跑鞋管理页面
        composable(Screen.RunningShoes.route) {
            RunningShoesManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddShoe = {
                    navController.navigate(Screen.AddEditRunningShoe.createRoute())
                },
                onNavigateToDetail = { shoeId ->
                    navController.navigate(Screen.RunningShoeDetail.createRoute(shoeId))
                }
            )
        }

        // 跑鞋详情页面
        composable(
            route = Screen.RunningShoeDetail.route,
            arguments = listOf(
                navArgument("shoeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shoeId = backStackEntry.arguments?.getString("shoeId") ?: return@composable
            RunningShoeDetailScreen(
                shoeId = shoeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.AddEditRunningShoe.createRoute(id))
                },
                onNavigateToLinkedRecords = { id ->
                    navController.navigate(Screen.LinkedRunRecords.createRoute(id))
                },
                onNavigateToBatchLink = { _ ->
                    // BatchLink is handled as a BottomSheet within the detail screen
                    // This callback is kept for potential future navigation
                }
            )
        }

        // 添加/编辑跑鞋页面
        composable(
            route = Screen.AddEditRunningShoe.route,
            arguments = listOf(
                navArgument("shoeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val shoeId = backStackEntry.arguments?.getString("shoeId")
            AddEditRunningShoeScreen(
                shoeId = shoeId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 跑鞋关联记录列表页面
        composable(
            route = Screen.LinkedRunRecords.route,
            arguments = listOf(
                navArgument("shoeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shoeId = backStackEntry.arguments?.getString("shoeId") ?: return@composable
            val context = LocalContext.current
            LinkedRunRecordsListScreen(
                shoeId = shoeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRunDetail = { workoutId ->
                    context.startActivity(RunDetailActivity.createIntent(context, workoutId))
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

