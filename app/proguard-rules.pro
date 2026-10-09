# BolPaisa ProGuard / R8 Rules

# Retain ZXing Core classes
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Retain Room Database & Entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class com.bolpaisa.app.data.** { *; }

# Retain BolPaisa Licensing & Security
-keep class com.bolpaisa.app.licensing.** { *; }
