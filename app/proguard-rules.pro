# Meizu MYVU SDK ProGuard Rules

# Keep SDK classes
-keep class me.panny777.myvu.** { *; }
-dontwarn me.panny777.myvu.**

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Navigation
-keep class * extends androidx.fragment.app.Fragment{}
-keep class * extends androidx.navigation.fragment.NavHostFragment{}

# Keep Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
