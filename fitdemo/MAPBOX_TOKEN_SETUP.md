# 🔑 Mapbox Downloads Token配置指南

## 问题说明

在编译时遇到以下错误：
```
Could not GET 'https://api.mapbox.com/downloads/v2/releases/maven/...'
Received status code 401 from server: Unauthorized
```

这是因为Mapbox SDK需要特殊的下载令牌（Downloads Token）进行认证。

## ⚡ 快速配置步骤

### 1️⃣ 访问Mapbox账号页面
打开浏览器访问：https://account.mapbox.com/access-tokens/

### 2️⃣ 创建Secret Token
1. 点击 **"Create a token"** 按钮
2. 在Token名称中输入：`Downloads Token`
3. 在**Secret scopes**部分，勾选：
   - ✅ `DOWNLOADS:READ`
4. 点击 **"Create token"** 按钮

### 3️⃣ 复制Token
- Token格式类似：`sk.eyJ1Ijoib3Rlcm1hbiIsImEiOiJjbWt2OGMx...`
- ⚠️ **重要**: Secret Token只显示一次，请立即复制保存！

### 4️⃣ 配置Token（选择一种方式）

#### 方式A：环境变量（推荐）

**Windows PowerShell:**
```powershell
$env:MAPBOX_DOWNLOADS_TOKEN="sk.your_token_here"
```

**Windows CMD:**
```cmd
set MAPBOX_DOWNLOADS_TOKEN=sk.your_token_here
```

**Mac/Linux:**
```bash
export MAPBOX_DOWNLOADS_TOKEN=sk.your_token_here
```

#### 方式B：gradle.properties文件

在项目根目录的`gradle.properties`文件中添加：
```properties
MAPBOX_DOWNLOADS_TOKEN=sk.your_token_here
```

#### 方式C：settings.gradle.kts直接配置

修改`settings.gradle.kts`中的Mapbox配置：
```kotlin
maven {
    url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    credentials {
        username = "mapbox"
        password = "sk.your_token_here"  // 直接填写你的token
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

### 5️⃣ 重新编译

配置完成后，重新运行：
```bash
./gradlew :fitdemo:assembleDebug
```

## 🎯 完整配置示例

### 示例1：使用环境变量

1. 设置环境变量
```powershell
$env:MAPBOX_DOWNLOADS_TOKEN="YOUR_MAPBOX_SECRET_TOKEN"
```

2. settings.gradle.kts保持默认配置（已配置）
```kotlin
maven {
    url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    credentials {
        username = "mapbox"
        password = providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN").orNull 
            ?: "fallback_token"
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

3. 编译
```bash
./gradlew :fitdemo:assembleDebug
```

### 示例2：直接在代码中配置（适合测试）

修改`settings.gradle.kts`第70行左右：
```kotlin
maven {
    url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    credentials {
        username = "mapbox"
        password = "YOUR_MAPBOX_SECRET_TOKEN"
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

⚠️ 注意：此方式会将token提交到版本控制，仅适合个人测试项目。

## ✅ 验证配置

配置成功后，你应该看到：
```
BUILD SUCCESSFUL in 30s
```

并且能够成功安装：
```bash
./gradlew :fitdemo:installDebug
```

## 🆚 Token类型对比

| Token类型 | 用途 | 位置 | 格式 |
|----------|------|------|------|
| **Access Token** | 地图API调用 | AndroidManifest.xml | `pk.xxx...` |
| **Downloads Token** | SDK下载认证 | settings.gradle.kts | `sk.xxx...` |

两种Token都需要，但用途不同：
- ✅ Access Token已在AndroidManifest中配置
- ⚠️ Downloads Token需要你现在配置

## 🔒 安全建议

1. **不要**将Secret Token提交到公开的Git仓库
2. **推荐**使用环境变量或`gradle.properties`（加入.gitignore）
3. **定期**更新和轮换tokens
4. **限制**token的scope到最小权限

## ❓ 常见问题

### Q: 我已经有Mapbox账号，在哪创建Downloads Token？
A: 登录后访问 https://account.mapbox.com/access-tokens/，点击"Create a token"，勾选DOWNLOADS:READ scope。

### Q: 免费账号可以用吗？
A: 可以！免费账号每月有50,000次地图加载额度，足够个人开发使用。

### Q: 设置环境变量后还是401错误？
A: 确保：
1. Token以`sk.`开头（Secret Token）
2. 重启终端/IDE让环境变量生效
3. 运行`echo $env:MAPBOX_DOWNLOADS_TOKEN`验证变量已设置

### Q: 公司网络SSL证书问题？
A: 可以尝试：
1. 导入公司SSL证书到Java keystore
2. 配置Gradle使用公司代理
3. 或使用方式C直接在settings.gradle.kts中配置

## 📚 参考文档

- [Mapbox Android Installation](https://docs.mapbox.com/android/maps/guides/install/)
- [Mapbox Token Management](https://docs.mapbox.com/accounts/guides/tokens/)
- [Gradle Credentials](https://docs.gradle.org/current/userguide/declaring_repositories.html)

## 🚀 下一步

配置完Downloads Token后：
1. 编译项目：`./gradlew :fitdemo:assembleDebug`
2. 安装到设备：`./gradlew :fitdemo:installDebug`
3. 打开应用，选择FIT文件
4. 点击"查看地图轨迹"按钮
5. 欣赏你的跑步轨迹在地图上！🎉

