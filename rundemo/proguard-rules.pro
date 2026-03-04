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

# Aggressive obfuscation for internal implementation
-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

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
