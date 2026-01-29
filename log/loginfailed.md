08:40:55.155  8495-18129 okhttp.OkHttpClient              I  --> POST https://yayarun.cn/sys/api/user/login/passwordLogin
08:40:55.155  8495-18129 okhttp.OkHttpClient              I  Content-Length: 343
08:40:55.156  8495-18129 okhttp.OkHttpClient              I  Content-Type: application/json
08:40:55.156  8495-18129 okhttp.OkHttpClient              I 
{
  "body": {
    "UserLoginRequestDto": [
      {
        "deviceId": "43ff9e34cdc24c30",
        "loginType": "PASSWORD",
        "password": "6592e67b4b52a43826d5d889afa959f5",
        "phoneNumber": "17512081100",
        "sceneType": "LOGIN"
      }
    ]
  },
  "head": {
    "appKey": "1jns01o9lksa12",
    "sign": "9d9c61920cc0cc1add34a8d782b274a03a4f121496e583f9bbe37c0e6c05334d",
    "timestamp": "1769647255137",
    "token": "",
    "userId": ""
  }
}

08:40:55.156  8495-18129 okhttp.OkHttpClient              I  --> END POST (343-byte body)
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  <-- 200 https://yayarun.cn/sys/api/user/login/passwordLogin (362ms)
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Server: nginx
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Date: Thu, 29 Jan 2026 00:40:56 GMT
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Content-Type: application/json
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Transfer-Encoding: chunked
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Connection: keep-alive
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Vary: Accept-Encoding
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-Content-Type-Options: nosniff
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-XSS-Protection: 1; mode=block
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Cache-Control: no-cache, no-store, max-age=0, must-revalidate
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Pragma: no-cache
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Expires: 0
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-Frame-Options: DENY
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-Frame-Options: SAMEORIGIN
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-Content-Type-Options: nosniff
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  X-XSS-Protection: 1; mode=block
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Referrer-Policy: no-referrer-when-downgrade
08:40:55.519  8495-18129 okhttp.OkHttpClient              I  Strict-Transport-Security: max-age=31536000
08:40:55.520  8495-18129 okhttp.OkHttpClient              I  {"code":"000C","msg":"请求无效","data":null}
08:40:55.520  8495-18129 okhttp.OkHttpClient              I  <-- END HTTP (48-byte body)