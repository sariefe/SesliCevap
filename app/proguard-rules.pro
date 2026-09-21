# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Moshi Models
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.data.remote.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.entity.** { *; }
-dontwarn androidx.room.paging.**

# Timber & Coroutines
-dontwarn timber.log.**

# SQLCipher (sqlcipher-android)
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
