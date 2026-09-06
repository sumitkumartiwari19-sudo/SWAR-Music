# Add project specific ProGuard rules here.

# NewPipe Extractor rules
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# HTML & JSON Parsers used by NewPipe Extractor
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**

# OkHttp & Okio
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * extends okhttp3.internal.ws.WebSocketReader { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
