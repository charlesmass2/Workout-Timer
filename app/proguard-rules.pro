# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.shizen.workouttimer.data.** {
    *** Companion;
}
-keepclasseswithmembers class io.shizen.workouttimer.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
