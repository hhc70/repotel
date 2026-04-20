
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep class coil.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn org.conscrypt.**
-dontwarn javax.annotation.**
