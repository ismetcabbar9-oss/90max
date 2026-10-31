# kotlinx.serialization: @Serializable sınıfların serializer'larını koru
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.ninetymax.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ninetymax.app.**$$serializer { *; }
