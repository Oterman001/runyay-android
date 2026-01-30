package com.oterman.rundemo.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.oterman.rundemo.presentation.feature.auth.forgotpassword.ForgotPasswordScreen
import com.oterman.rundemo.presentation.feature.auth.login.LoginScreen
import com.oterman.rundemo.presentation.feature.auth.register.RegisterScreen
import com.oterman.rundemo.presentation.feature.home.HomeScreen
import com.oterman.rundemo.presentation.feature.welcome.WelcomeScreen

/**
 * 应用导航图
 * 定义应用的导航结构和页面跳转逻辑
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
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
                    // 登录成功后导航到主页面
                    navController.navigate(Screen.Home.route) {
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
        
        // 主页面（登录成功后的页面）
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route)
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
                    // 注册成功后导航到主页面
                    navController.navigate(Screen.Home.route) {
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

