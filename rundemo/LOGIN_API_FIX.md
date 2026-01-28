# 登录API请求修复说明

## 问题诊断

通过对比iOS和Android的网络请求日志，发现以下关键差异：

### iOS请求（正确）：
```
URL: https://yayarun.cn/sys/api/user/login/passwordLogin
请求体结构：
{
  "head": {
    "appKey": "1jns01o9lksa12",
    "sign": "207351227b9076223a66d0f94b9bdbcfdd7f7087071bc090c322ece1acd42672",
    "timestamp": "1761717897204",
    "userId": "",
    "token": ""
  },
  "body": {
    "UserLoginRequestDto": [{
      "password": "6592e67b4b52a43826d5d889afa959f5",
      "deviceId": "7D2889EA-22B9-4ADE-A00D-90AFF0F19114",
      "loginType": "PASSWORD",
      "sceneType": "LOGIN",
      "phoneNumber": "19183959302"
    }]
  }
}
```

### Android请求（错误）：
```
URL: https://yayarun.cn/sys/api/user/login  ❌ 路径错误
请求体结构：
{
  "dtoName": "UserLoginRequestDto",  ❌ 结构错误
  "data": [{...}]
}
```

## 问题总结

1. **API路径错误**：Android使用 `/api/user/login`，应该是 `/api/user/login/passwordLogin`
2. **请求结构错误**：缺少 `head` 字段，应该包含 appKey、sign、timestamp 等
3. **请求体格式错误**：应该是 `head + body` 结构，而不是简单的 `dtoName + data`

## 修复内容

### 1. 更新 `UserLoginRequest.kt`
- ✅ 添加 `RequestHead` 数据类（包含 appKey、sign、timestamp、token、userId）
- ✅ 修改 `BaseRequest` 结构为 `head + body` 格式
- ✅ body 使用 `Map<String, List<T>>` 结构

### 2. 更新 `SecurityUtils.kt`
- ✅ 添加 `sha256()` 加密方法
- ✅ 添加 `getTimestamp()` 获取当前时间戳
- ✅ 添加 `generateSign()` 生成请求签名

### 3. 更新 `Constants.kt`
- ✅ 设置正确的 `BASE_URL`: `https://yayarun.cn/sys/`
- ✅ 添加 `APP_KEY`: `1jns01o9lksa12`

### 4. 更新 `UserApi.kt`
- ✅ 修改API路径为 `api/user/login/passwordLogin`

### 5. 更新 `UserRepository.kt`
- ✅ 生成请求头（包含签名）
- ✅ 构建符合iOS格式的完整请求体
- ✅ 请求体包含 `head` 和 `body` 两部分

## 修复后的请求结构

现在Android的请求与iOS完全一致：

```kotlin
// 构建请求头
val requestHead = RequestHead(
    appKey = "1jns01o9lksa12",
    timestamp = "当前时间戳",
    sign = "SHA256签名",
    token = "用户token（登录时为空）",
    userId = "用户ID（登录时为空）"
)

// 构建完整请求
val request = BaseRequest(
    head = requestHead,
    body = mapOf("UserLoginRequestDto" to listOf(requestDto))
)
```

## 测试验证

修复后的请求将发送到：
- URL: `https://yayarun.cn/sys/api/user/login/passwordLogin`
- Content-Type: `application/json`
- Body: 包含完整的 `head` 和 `body` 结构

请重新编译并测试登录功能，现在应该能够成功调用API了！

## 关键文件清单

修改的文件：
1. `rundemo/src/main/java/com/oterman/rundemo/data/network/dto/request/UserLoginRequest.kt`
2. `rundemo/src/main/java/com/oterman/rundemo/util/SecurityUtils.kt`
3. `rundemo/src/main/java/com/oterman/rundemo/util/Constants.kt`
4. `rundemo/src/main/java/com/oterman/rundemo/data/network/api/UserApi.kt`
5. `rundemo/src/main/java/com/oterman/rundemo/data/repository/UserRepository.kt`

