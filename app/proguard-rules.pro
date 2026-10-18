# Keep Kotlinx Serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.galaxy.worddictionary.**$$serializer { *** Companion; }
-keepclassmembers class com.galaxy.worddictionary.** {
    *** Companion;
}

# Keep model data classes for serialization
-keep class com.galaxy.worddictionary.data.model.** { *; }
