import java.text.SimpleDateFormat
import java.util.Date

val gitHash: String = try {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=6", "HEAD"))
    process.inputStream.bufferedReader().readLine()?.trim() ?: "unknown"
} catch (e: Exception) { "unknown" }

val buildDate: String = SimpleDateFormat("yyMMdd").format(Date())

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "com.oterman.rundemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.yayrun.android"
        minSdk = 29
        targetSdk = 36
        versionCode = 10006  // 00-00-00
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 高德地图 API Key（通过 gradle.properties 或环境变量配置）
        manifestPlaceholders["AMAP_API_KEY"] = providers.gradleProperty("AMAP_API_KEY").getOrElse("YOUR_AMAP_API_KEY")

        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(providers.gradleProperty("RUNDEMO_STORE_FILE").get())
            storePassword = providers.gradleProperty("RUNDEMO_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RUNDEMO_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RUNDEMO_KEY_PASSWORD").get()
        }
        create("release") {
            storeFile = file(providers.gradleProperty("RUNDEMO_RELEASE_STORE_FILE").get())
            storePassword = providers.gradleProperty("RUNDEMO_RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RUNDEMO_RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RUNDEMO_RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions += listOf("channel", "mapVariant")
    productFlavors {
        create("internal") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"dev-internal\"")
        }
        create("fir") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"fir\"")
        }
        create("xiaomi") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"xiaomi\"")
        }
        create("oppo") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"oppo\"")
        }
        create("vivo") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"vivo\"")
        }
        create("huawei") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"huawei\"")
        }
        create("honor") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"honor\"")
        }

        create("google") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"google\"")
        }
        create("tencent") {
            dimension = "channel"
            buildConfigField("String", "UMENG_CHANNEL", "\"tencent\"")
        }

        // ── mapVariant 维度 ──
        create("mapboxOnly") { dimension = "mapVariant" }
        create("allMaps")    { dimension = "mapVariant" }
        create("amapOnly")   { dimension = "mapVariant" }
    }

    sourceSets {
        named("mapboxOnly") { java.srcDir("src/mapProvider_mapbox/java") }
        named("allMaps") {
            java.srcDir("src/mapProvider_mapbox/java")
            java.srcDir("src/mapProvider_amap/java")
        }
        named("amapOnly") { java.srcDir("src/mapProvider_amap/java") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        val btShort = if (variant.buildType.name == "release") "r" else "d"
        val channelName = variant.productFlavors.first { it.dimension == "channel" }.name
        val mapShort = when {
            variant.productFlavors.any { it.name == "mapboxOnly" } -> "mb"
            variant.productFlavors.any { it.name == "amapOnly" }   -> "am"
            else -> "all"
        }
        outputs.all {
            val out = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            out.outputFileName =
                "ry-${variant.versionName}-${variant.versionCode}" +
                "-${channelName}-${mapShort}-${buildDate}-${gitHash}-${btShort}.apk"
        }
    }
}

// 裁减无意义的变体组合，减少构建变体数量
androidComponents {
    beforeVariants { variantBuilder ->
        val channel = variantBuilder.productFlavors.first { it.first == "channel" }.second
        val mapVar  = variantBuilder.productFlavors.first { it.first == "mapVariant" }.second
        variantBuilder.enable = when {
            // Google Play 只发 mapboxOnly（高德在境外不可用）
            channel == "google" && mapVar != "mapboxOnly" -> false
            // 国内渠道不单独发 mapboxOnly（Mapbox 在国内访问不稳定）
            channel in listOf("xiaomi", "oppo", "vivo", "huawei", "honor", "tencent", "fir")
                && mapVar == "mapboxOnly" -> false
            else -> true
        }
    }
}

// Room schema export directory
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Material Icons Extended (包含 Visibility 图标)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    
    // Retrofit网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // 显式声明 Gson 版本，确保 JsonParser.parseString() 在所有 flavor 下可用
    // （amapOnly 无 Mapbox，若不声明则只有 converter-gson 带入的旧版 Gson 2.8.5）
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // DataStore (替代SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Garmin FIT SDK
    implementation("com.garmin:fit:21.171.0")

    // Offline latitude/longitude -> IANA timezone lookup for imported FIT activities
//    implementation("net.iakovlev:timeshape:2025b.28") {
//        exclude(group = "com.github.luben", module = "zstd-jni")
//    }
//    implementation("com.github.luben:zstd-jni:1.5.5-11@aar")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // uCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.9")

    // Training calendar (kizitonwose/calendar)
    implementation("com.kizitonwose.calendar:compose:2.6.1")

    // Mapbox Maps SDK（仅 mapboxOnly / allMaps flavor）
    add("mapboxOnlyImplementation", "com.mapbox.maps:android:11.18.0")
    add("allMapsImplementation",    "com.mapbox.maps:android:11.18.0")

    // 高德地图 3D SDK（仅 amapOnly / allMaps flavor）
    add("amapOnlyImplementation",   "com.amap.api:3dmap:10.0.600")
    add("allMapsImplementation",    "com.amap.api:3dmap:10.0.600")

    // Vico Charts (Compose)
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.0-beta.1")

    // Reorderable LazyColumn (drag-and-drop sorting)
    implementation("sh.calvin.reorderable:reorderable:2.4.0")

    // ZIP with password encryption for log export
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // umeng
//    implementation("com.umeng.umsdk:common:+")
//    implementation("com.umeng.umsdk:asms:+")
    implementation("com.umeng.umsdk:common:9.4.7")
    implementation("com.umeng.umsdk:asms:1.4.0")
    // 友盟 U-APM 性能 & 崩溃监控（随 UMConfigure.init 自动激活）
    implementation("com.umeng.umsdk:apm:1.9.0")

    // 腾讯 Bugly 崩溃 / ANR 上报
    implementation("com.tencent.bugly:crashreport:4.1.9.3")

    // Firebase Crashlytics + Performance（仅 google 渠道）
    // 前置条件：在模块根目录放置 google-services.json，并在 plugins 块启用 google-services 与 firebase-crashlytics 插件
    add("googleImplementation", platform("com.google.firebase:firebase-bom:33.7.0"))
    add("googleImplementation", "com.google.firebase:firebase-crashlytics-ktx")
    add("googleImplementation", "com.google.firebase:firebase-perf-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
