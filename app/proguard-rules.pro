# libbox (gomobile) از طریق JNI به همه‌ی کلاس‌ها دسترسی دارد
-keep class io.nekohasekai.libbox.** { *; }
-keep class go.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.myvpn.app.**$$serializer { *; }
-keepclassmembers class com.myvpn.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.myvpn.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
