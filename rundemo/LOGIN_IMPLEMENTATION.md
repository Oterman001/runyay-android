# Android 登录模块实现说明

## 概述

本项目实现了基于 Jetpack Compose + MVVM 架构的完整登录功能模块，与 iOS 版本保持功能和界面布局一致，采用 Material3 设计规范。

## 已实现的功能

### ✅ 核心功能
- **手机号密码登录** - 支持手机号+密码方式登录
- **输入验证** - 实时验证手机号格式（正则：^1[3-9]\\d{9}$）和密码长度（≥6位）
- **密码可见性切换** - 支持显示/隐藏密码
- **用户协议勾选** - 必须勾选协议才能登录，未勾选时触发抖动动画
- **加载状态** - 登录过程中显示加载指示器
- **错误处理** - 完善的错误提示和剩余尝试次数显示
- **账户锁定提示** - 密码错误次数过多时显示锁定对话框
- **本地存储** - 用户信息和Token持久化存储
- **Token管理** - 自动管理Token过期时间

### 📁 项目结构

```
rundemo/src/main/java/com/oterman/rundemo/
├── data/                                  # 数据层
│   ├── network/                           # 网络请求
│   │   ├── api/
│   │   │   └── UserApi.kt                # 用户API接口
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   └── UserLoginRequest.kt   # 登录请求DTO
│   │   │   └── response/
│   │   │       ├── BaseResponse.kt       # 基础响应
│   │   │       └── UserLoginResponse.kt  # 登录响应DTO
│   │   ├── interceptor/
│   │   │   └── AuthInterceptor.kt        # 认证拦截器
│   │   └── RetrofitClient.kt             # Retrofit配置
│   ├── repository/
│   │   └── UserRepository.kt             # 用户数据仓库
│   └── local/
│       └── PreferencesManager.kt         # 本地存储管理
├── domain/                                # 业务逻辑层
│   └── model/
│       └── UserInfo.kt                   # 用户信息模型
├── presentation/                          # 表现层
│   ├── navigation/
│   │   ├── NavGraph.kt                   # 导航图
│   │   └── Screen.kt                     # 路由定义
│   ├── feature/
│   │   └── auth/
│   │       └── login/
│   │           ├── LoginScreen.kt        # 登录界面
│   │           ├── LoginViewModel.kt     # 登录ViewModel
│   │           ├── LoginUiState.kt       # UI状态
│   │           └── LoginViewModelFactory.kt
│   └── components/                        # 通用UI组件
│       ├── LoadingButton.kt              # 加载按钮
│       ├── PasswordTextField.kt          # 密码输入框
│       ├── TermsCheckbox.kt              # 协议勾选框
│       └── ShakeAnimation.kt             # 抖动动画
├── util/                                  # 工具类
│   ├── ValidationUtils.kt                # 验证工具
│   ├── SecurityUtils.kt                  # 加密工具（MD5）
│   └── Constants.kt                      # 常量定义
└── MainActivity.kt                        # 主Activity
```

## 技术栈

- **UI框架**: Jetpack Compose + Material3
- **架构模式**: MVVM + StateFlow
- **导航方案**: Jetpack Compose Navigation
- **网络请求**: Retrofit 2.9.0 + OkHttp 4.12.0
- **JSON解析**: Gson
- **依赖注入**: 手动依赖注入（通过ViewModelFactory）
- **异步处理**: Kotlin Coroutines + Flow

## 核心组件说明

### 1. LoginViewModel
- 管理登录界面的所有状态（手机号、密码、错误信息等）
- 使用 StateFlow 实现响应式UI更新
- 处理登录逻辑和错误处理
- 实现协议检查和抖动动画触发

### 2. LoginScreen
- 采用 Material3 设计规范
- 响应式布局，支持不同屏幕尺寸
- 实时输入验证和错误提示
- 优雅的错误对话框显示

### 3. UserRepository
- 封装网络请求和本地存储
- 自动处理密码MD5加密
- 统一的错误处理机制
- 管理用户登录状态

### 4. PreferencesManager
- 使用 SharedPreferences 存储用户信息
- Token过期时间管理
- 提供便捷的存储和读取方法

## 与 iOS 版本的对应关系

| iOS | Android |
|-----|---------|
| `PhoneLoginView` | `LoginScreen` |
| `PhoneLoginViewModel` | `LoginViewModel` |
| `UserService` | `UserRepository` + `UserApi` |
| `AccountManager` | `PreferencesManager` |
| `UserLoginResponseDto` | `UserLoginResponse` |
| `AppNavigationCoordinator` | Compose Navigation |

## 使用说明

### 配置API地址

在 `RetrofitClient.kt` 中修改 `BASE_URL`：

```kotlin
private const val BASE_URL = "https://your-api-base-url.com/"
```

### 运行项目

1. 同步 Gradle 依赖
2. 确保连接了设备或模拟器
3. 点击 Run 按钮运行应用

### 测试登录

1. 输入11位手机号（以1开头，第二位3-9）
2. 输入至少6位密码
3. 勾选用户协议
4. 点击登录按钮

## 关键特性

### 1. 输入验证
- **实时验证**：输入时即时显示错误提示
- **正则表达式**：严格的手机号格式验证
- **密码长度**：确保密码至少6位

### 2. 安全性
- **MD5加密**：密码在传输前进行MD5加密
- **Token管理**：自动保存和刷新Token
- **设备ID**：使用Android ID作为设备标识

### 3. 用户体验
- **加载指示器**：登录过程中显示进度
- **错误提示**：清晰的错误信息和剩余尝试次数
- **抖动动画**：未勾选协议时的视觉反馈
- **渐变按钮**：美观的按钮设计

### 4. 状态管理
- **响应式UI**：使用StateFlow自动更新界面
- **状态持久化**：屏幕旋转时保持输入状态
- **登录状态**：自动判断是否需要登录

## 待实现功能

以下功能已预留接口，后续实现：
- ⏳ 用户注册流程
- ⏳ 忘记密码功能
- ⏳ 图形验证码集成
- ⏳ 用户协议和隐私政策页面
- ⏳ 验证码登录方式

## 注意事项

1. **网络权限**：确保在 AndroidManifest.xml 中添加了网络权限
2. **API地址**：需要配置正确的后端API地址
3. **依赖版本**：确保所有依赖版本兼容
4. **测试**：建议在真机上测试网络请求功能

## 依赖版本

```gradle
// Retrofit网络请求
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Compose Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Lifecycle & ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## 总结

本实现完全遵循了计划文档中的设计方案，采用了现代Android开发的最佳实践，代码结构清晰，易于维护和扩展。所有核心功能都已实现并可正常工作，为后续的注册、忘记密码等功能奠定了良好的基础。

