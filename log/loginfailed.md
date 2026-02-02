2026-02-01 15:20:07.328 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  I  开始获取授权URL, platform=COROS, userId=U17542070721940021 (DataSourceRepository.kt:107)
2026-02-01 15:20:07.331 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  D  发送绑定请求, dtoName=CorosBindRequest (DataSourceRepository.kt:120)
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> POST https://yayarun.cn/sys/coros/bind
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Length: 339
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Authorization: Bearer eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA==
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Type: application/json
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  {"body":{"CorosBindRequest":[{"userId":"U17542070721940021"}]},"head":{"appKey":"1jns01o9lksa12","sign":"d72ba2e0d35540af5d3c5832c7b9b22e1886b9a64e80723f225b8a3ec19c76d4","timestamp":"1769930407330","token":"eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA\u003d\u003d","userId":"U17542070721940021"}}
2026-02-01 15:20:07.335 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> END POST (339-byte body)
2026-02-01 15:20:07.457 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  <-- 200 https://yayarun.cn/sys/coros/bind (121ms)
2026-02-01 15:20:07.457 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Server: nginx
2026-02-01 15:20:07.457 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Date: Sun, 01 Feb 2026 07:20:07 GMT
2026-02-01 15:20:07.457 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Type: application/json
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Transfer-Encoding: chunked
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Connection: keep-alive
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Vary: Accept-Encoding
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Content-Type-Options: nosniff
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-XSS-Protection: 1; mode=block
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Cache-Control: no-cache, no-store, max-age=0, must-revalidate
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Pragma: no-cache
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Expires: 0
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Frame-Options: DENY
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Frame-Options: SAMEORIGIN
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Content-Type-Options: nosniff
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-XSS-Protection: 1; mode=block
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Referrer-Policy: no-referrer-when-downgrade
2026-02-01 15:20:07.458 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Strict-Transport-Security: max-age=31536000
2026-02-01 15:20:07.459 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  {"code":"0000","msg":"OK","data":{"CorosBindResponse":[{"authUrl":"https://open.coros.com/oauth2/authorize?client_id=3c84bd82103e43d1badcd1c40bcdaa6a&redirect_uri=https%3A%2F%2Fyayarun.cn%2Foauth%2Fcoros%2Fcallback&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG%2BemODB0tXbu&response_type=code"}]}}
2026-02-01 15:20:07.459 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  <-- END HTTP (319-byte body)
2026-02-01 15:20:07.461 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  D  收到绑定响应, code=0000, msg=OK (DataSourceRepository.kt:129)
2026-02-01 15:20:07.463 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  D  高驰授权URL: https://open.coros.com/oauth2/authorize?client_id=3c84bd82103e43d1badcd1c40bcdaa6a&redirect_uri=https%3A%2F%2Fyayarun.cn%2Foauth%2Fcoros%2Fcallback&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG%2BemODB0tXbu&response_type=code (DataSourceRepository.kt:141)
2026-02-01 15:20:07.464 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  I  获取授权URL成功, platform=COROS (DataSourceRepository.kt:148)
2026-02-01 15:20:07.835 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  页面开始加载: https://open.coros.com/oauth2/authorize?client_id=3c84bd82103e43d1badcd1c40bcdaa6a&redirect_uri=https://yayarun.cn/oauth/coros/callback&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL/iGDk9aTVmF/p0c5/pi/YG+emODB0tXbu&response_type=code (OAuthWebViewScreen.kt:179)
2026-02-01 15:20:07.863 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  页面加载完成: https://open.coros.com/oauth2/authorize?client_id=3c84bd82103e43d1badcd1c40bcdaa6a&redirect_uri=https://yayarun.cn/oauth/coros/callback&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL/iGDk9aTVmF/p0c5/pi/YG+emODB0tXbu&response_type=code (OAuthWebViewScreen.kt:185)



2026-02-01 15:20:18.486 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  WebView导航到: https://yayarun.cn/oauth/coros/callback?code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (OAuthWebViewScreen.kt:166)

2026-02-01 15:20:18.487 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  I  检测到回调URL: https://yayarun.cn/oauth/coros/callback?code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (OAuthWebViewScreen.kt:227)
2026-02-01 15:20:18.488 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  开始解析回调URL: https://yayarun.cn/oauth/coros/callback?code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (OAuthWebViewScreen.kt:246)
2026-02-01 15:20:18.488 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  平台类型: COROS (OAuthWebViewScreen.kt:247)
2026-02-01 15:20:18.489 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  I  高驰回调参数(原始编码): code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7, state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (OAuthWebViewScreen.kt:281)
2026-02-01 15:20:18.490 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  I  高驰授权成功，回调处理 (OAuthWebViewScreen.kt:288)
2026-02-01 15:20:18.490 29780-29780 XRUN_DataSourceDetailVM com.oterman.rundemo                  I  处理OAuth回调: platform=COROS, params=OAuth2(code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7, state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu) (DataSourceDetailViewModel.kt:101)
2026-02-01 15:20:18.491 29780-29780 XRUN_DataSourceDetailVM com.oterman.rundemo                  D  调用高驰回调处理, code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7, state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (DataSourceDetailViewModel.kt:121)
2026-02-01 15:20:18.492 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  I  处理高驰OAuth回调: code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7, state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (DataSourceRepository.kt:227)
2026-02-01 15:20:18.493 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  D  发送高驰回调请求 (DataSourceRepository.kt:238)
2026-02-01 15:20:18.498 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> POST https://yayarun.cn/sys/coros/callback
2026-02-01 15:20:18.498 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Length: 472
2026-02-01 15:20:18.498 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Authorization: Bearer eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA==
2026-02-01 15:20:18.498 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Type: application/json
2026-02-01 15:20:18.498 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  {"body":{"CorosCallbackRequest":[{"code":"rg1-cbc4b5d19f0809e4132a19e7b61e7ae7","state":"eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu","userId":"U17542070721940021"}]},"head":{"appKey":"1jns01o9lksa12","sign":"e1c895632671cecd7710fee12d59846faac058ebe92fc10384fdec5deb1f7f55","timestamp":"1769930418492","token":"eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA\u003d\u003d","userId":"U17542070721940021"}}
2026-02-01 15:20:18.499 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> END POST (472-byte body)
2026-02-01 15:20:18.538 29780-29780 XRUN_OAuthWebView       com.oterman.rundemo                  D  页面加载完成: https://yayarun.cn/oauth/coros/callback?code=rg1-cbc4b5d19f0809e4132a19e7b61e7ae7&state=eRVWidssAAQyKDgMWzr5TwQIpaizYYL%2FiGDk9aTVmF%2Fp0c5%2Fpi%2FYG+emODB0tXbu (OAuthWebViewScreen.kt:185)
2026-02-01 15:20:18.541 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  I  开始查询平台绑定状态, userId=U17542070721940021 (DataSourceRepository.kt:49)
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> POST https://yayarun.cn/sys/api/user/profile/platform/status
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Length: 344
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  Authorization: Bearer eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA==
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Type: application/json
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  {"body":{"PlatformStatusRequest":[{"userId":"U17542070721940021"}]},"head":{"appKey":"1jns01o9lksa12","sign":"1e844782ca5ea513b5a5bf14e8028daf8412d0d406818e64c4b26cb9b0990212","timestamp":"1769930418542","token":"eRVWidssAAQyKDgMWzr5T7l5O8BEvRLWdhPEZwaqeIz0HGdRpORgCEXoHnxPCMXq7q+txPeVBySHDVpAXedeFA\u003d\u003d","userId":"U17542070721940021"}}
2026-02-01 15:20:18.546 29780-30664 okhttp.OkHttpClient     com.oterman.rundemo                  I  --> END POST (344-byte body)
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  <-- 200 https://yayarun.cn/sys/coros/callback (99ms)
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Server: nginx
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Date: Sun, 01 Feb 2026 07:20:18 GMT
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Content-Type: application/json
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Transfer-Encoding: chunked
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Connection: keep-alive
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Vary: Accept-Encoding
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Content-Type-Options: nosniff
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-XSS-Protection: 1; mode=block
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Cache-Control: no-cache, no-store, max-age=0, must-revalidate
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Pragma: no-cache
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Expires: 0
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Frame-Options: DENY
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Frame-Options: SAMEORIGIN
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-Content-Type-Options: nosniff
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  X-XSS-Protection: 1; mode=block
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Referrer-Policy: no-referrer-when-downgrade
2026-02-01 15:20:18.599 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  Strict-Transport-Security: max-age=31536000
2026-02-01 15:20:18.600 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  {"code":"0091","msg":"高驰State验证失败","data":null}
2026-02-01 15:20:18.600 29780-29979 okhttp.OkHttpClient     com.oterman.rundemo                  I  <-- END HTTP (59-byte body)
2026-02-01 15:20:18.601 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  D  高驰回调响应: code=0091, msg=高驰State验证失败 (DataSourceRepository.kt:242)
2026-02-01 15:20:18.602 29780-29973 XRUN_DataS...Repository com.oterman.rundemo                  W  高驰回调失败: 高驰State验证失败 (DataSourceRepository.kt:249)
