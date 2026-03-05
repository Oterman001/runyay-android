# Keep public API for VDOT calculators
-keep class com.oterman.rundemo.data.fit.VdotCalculator {
    public static double calculateFromDistanceAndTime(double, double, double, java.lang.Double, double, double);
    public static *** calculateFromSegments(...);
    public static *** calculateOverallVdot(...);
    public static double getVDot(double, double);
}
-keep class com.oterman.rundemo.data.fit.VdotSpeedCalculator { public *; }
-keep class com.oterman.rundemo.data.fit.AbilityZoneCalculator { public *; }
-keep class com.oterman.rundemo.data.fit.AbilityZoneCalculator$* { *; }

# Obfuscation settings
# Note: -repackageclasses and -overloadaggressively removed because they break
# Retrofit's reflection-based generic type resolution in R8 full mode.
# R8 repackages library classes (e.g. kotlin.coroutines.Continuation) but fails
# to update Signature attributes on kept interfaces, causing ClassCastException.
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class com.oterman.rundemo.util.RLog {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
}

# Keep Room entities
-keep class com.oterman.rundemo.data.local.entity.** { *; }

# Keep Retrofit interfaces
-keep interface com.oterman.rundemo.data.network.api.** { *; }

# Keep data classes used in API requests/responses
-keep class com.oterman.rundemo.data.network.dto.** { *; }

# Keep FitRecord and related data classes used by FitRecordProcessor
-keep class com.oterman.rundemo.data.fit.FitRecord { *; }
-keep class com.oterman.rundemo.data.fit.FitEventConverter$PauseEvent { *; }
-keep class com.oterman.rundemo.data.fit.FitFileParser { *; }
-keep class com.oterman.rundemo.data.fit.FitRecordProcessor { public *; }

# Standard Android / Kotlin rules
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Garmin FIT SDK
-keep class com.garmin.fit.** { *; }

# Gson / Retrofit
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Mapbox
-keep class com.mapbox.** { *; }
-dontwarn com.mapbox.**

#umeng
-keep class com.umeng.** {*;}

-keep class org.repackage.** {*;}

-keep class com.uyumao.** { *; }

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
#SDK需要引用导入工程的资源文件，通过了反射机制得到资源引用文件R.java，但是在开发者通过proguard等混淆/优化工具处理apk时，proguard可能会将R.java删除，如果遇到这个问题，请添加如下配置：
#-keep public class [您的应用包名].R$*{
#public static final int *;
#}