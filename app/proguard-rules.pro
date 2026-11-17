# kotlinx.serialization: keep generated serializers for our models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class app.teadroid.data.** {
    *** Companion;
}
-keepclasseswithmembers class app.teadroid.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
