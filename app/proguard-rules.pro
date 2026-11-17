# kotlinx.serialization: keep generated serializers for our models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class site.colebolebole.ezject.data.** {
    *** Companion;
}
-keepclasseswithmembers class site.colebolebole.ezject.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
