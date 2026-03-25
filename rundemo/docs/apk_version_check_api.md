



获取最新版本

**接口地址：** POST /api/version/app/getLatest

**Content-Type：** application/json

**接口说明：** 查询指定平台当前最新启用版本的信息及安装包下载链接。客户端启动时调用，用于检测是否有新版本可更新。

**请求参数（****body.GetLatestVersionRequest[0]****）**

| 字段名   | 类型   | 必填 | 说明                                  |
| -------- | ------ | ---- | ------------------------------------- |
| platform | String | 是   | 平台类型：ANDROID / IOS，不区分大小写 |

**请求示例**

JSON {  "head": {   "appKey": "your_app_key",   "sign": "your_sign",   "timestamp": "1700000000000"  },  "body": {   "GetLatestVersionRequest": [    {     "platform": "ANDROID"    }   ]  } }

*getLatest* *接口无需传* *token**（不要求用户登录）。*

**响应参数（****data.GetLatestVersionResponse[0]****）**

| 字段名       | 类型    | 说明                                         |
| ------------ | ------- | -------------------------------------------- |
| platform     | String  | 平台类型                                     |
| versionName  | String  | 版本名称，如 1.2.3                           |
| versionCode  | Integer | 版本号（整型），用于与本地版本比较大小       |
| changelog    | String  | 更新日志                                     |
| downloadUrl  | String  | 安装包完整下载地址（OSS 公开链接，永久有效） |
| forceUpgrade | boolean | 是否强制升级                                 |
| fileSize     | Long    | 安装包文件大小（字节）                       |
| ext1         | String  | 备用                                         |
| ext1         | String  | 备用                                         |
| ext1         | String  | 备用                                         |

**响应示例**

JSON {   "code": "0000",   "msg": "OK",   "data": {     "GetLatestVersionResponse": [       {         "platform": "ANDROID",         "versionName": "1.2.4",         "versionCode": 125,         "changelog": "1. 修复若干已知问题\n2. 优化跑步数据同步速度",         "downloadUrl": "https://yayarun.oss-cn-hangzhou.aliyuncs.com/apk/yayarun-v1.2.3.apk",         "fileSize": 45678901,         "forceUpgrade": **true**,         "ext1": **null**,         "ext1": **null**,         "ext3": **null**       }     ]   } }

**错误码**

| 错误码 | 说明                            |
| ------ | ------------------------------- |
| E1144  | 暂无可用版本                    |
| E1145  | 平台类型不能为空                |
| E1146  | 平台类型无效，仅支持ANDROID/IOS |

