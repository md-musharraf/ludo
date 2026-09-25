# Release shrinking / obfuscation rules.

# Strip verbose logging from release builds so no game internals reach logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Type-safe navigation routes are serialized by kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.example.ludo.**$$serializer { *; }
-keepclassmembers class com.example.ludo.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.ludo.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.example.ludo.** { *; }
