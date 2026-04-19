# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ============ Room ============
# Keep Room entities (required for database operations)
-keep class com.readtrack.data.local.entity.** { *; }
-keep class com.readtrack.data.local.dao.** { *; }

# ============ Kotlin Serialization ============
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes explicitly (R8 will find them via @Serializable but explicit is safer)
-keep,includedescriptorclasses class com.readtrack.domain.model.**$$serializer { *; }
-keepclassmembers class com.readtrack.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.readtrack.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============ Hilt ============
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ============ Coil ============
-dontwarn coil.**

# ============ OkHttp ============
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
