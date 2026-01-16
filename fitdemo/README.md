# FIT文件解析模块使用说明

## 功能概述

本模块实现了基于Garmin FIT SDK的跑步数据解析功能，用户可以从手机选择.fit文件，应用会自动解析并展示跑步摘要信息。

## 技术架构

- **架构模式**: MVVM (Model-View-ViewModel)
- **UI框架**: Jetpack Compose + Material 3
- **异步处理**: Kotlin Coroutines + Flow
- **FIT SDK**: Garmin FIT Java SDK 21.171.0

## 项目结构

```
fitdemo/
├── data/
│   ├── model/
│   │   ├── FitSummaryData.kt    # 数据模型
│   │   └── UiState.kt            # UI状态模型
│   └── repository/
│       └── FitFileRepository.kt  # FIT文件解析仓库
├── ui/
│   ├── components/               # 可复用UI组件
│   │   ├── InfoCard.kt
│   │   ├── InfoRow.kt
│   │   └── SectionHeader.kt
│   ├── screen/
│   │   └── FitFileScreen.kt      # 主界面
│   └── theme/                     # 主题配置
├── viewmodel/
│   └── FitViewModel.kt            # ViewModel层
└── MainActivity.kt                # 主Activity
```

## 展示的数据

### 1. 文件信息
- 文件类型
- 制造商
- 产品信息
- 序列号
- 创建时间
- 文件编号

### 2. 会话摘要
- 运动类型和子类型
- 开始时间
- 总用时和计时时间
- 总距离
- 总卡路里
- 平均/最大速度
- 平均/最快配速
- 平均/最大心率
- 平均/最大步频
- 总上升/下降
- 平均步幅

### 3. 轨迹信息
- GPS记录点总数
- 是否包含GPS数据
- 是否包含心率数据
- 是否包含步频数据

### 4. 区间数据（按Lap展示）
- 每个区间的详细信息
- 包括距离、时间、配速、心率等

### 5. 设备信息
- 制造商和产品
- 序列号
- 设备类型
- 硬件/软件版本

## 使用方法

1. 启动应用
2. 点击"选择FIT文件"按钮
3. 从系统文件选择器中选择.fit文件
4. 等待解析完成
5. 查看解析结果
6. 可点击"选择其他文件"继续解析其他文件

## 测试建议

### 准备测试文件
1. 从佳明设备导出.fit文件到手机
2. 可以使用多个不同活动的文件进行测试

### 测试场景
1. **正常场景**: 选择有效的.fit文件，验证所有数据正确展示
2. **异常场景**: 选择非.fit文件，验证错误提示
3. **边界场景**: 测试大文件、多区间文件等
4. **UI测试**: 检查不同屏幕尺寸下的显示效果

## Compose最佳实践应用

1. ✅ **单向数据流**: UI通过StateFlow观察状态
2. ✅ **状态提升**: ViewModel管理所有状态
3. ✅ **组件化**: 可复用的UI组件(InfoCard, InfoRow等)
4. ✅ **Material 3**: 使用最新的Material Design 3
5. ✅ **预览支持**: 提供@Preview用于开发调试
6. ✅ **响应式布局**: 使用LazyColumn处理长列表
7. ✅ **类型安全**: 使用sealed class定义UI状态
8. ✅ **异步处理**: viewModelScope + Dispatchers.IO

## 依赖说明

```kotlin
// Garmin FIT SDK
implementation("com.garmin:fit:21.171.0")

// ViewModel Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## 注意事项

1. 使用Storage Access Framework，无需申请存储权限
2. 文件解析在后台线程执行，不会阻塞UI
3. 所有异常都会被捕获并友好提示用户
4. 支持Android 13+（minSdk = 33）

## 构建和运行

```bash
# 同步Gradle依赖
./gradlew :fitdemo:build

# 安装到设备
./gradlew :fitdemo:installDebug

# 运行应用
adb shell am start -n com.oterman.fitdemo/.MainActivity
```

