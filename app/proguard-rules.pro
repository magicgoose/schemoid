-ignorewarnings
-dontwarn **
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class ** extends com.google.gson.reflect.TypeToken.TypeToken { *; }

-keep class magicgoose.common.annotation.*
-keep @magicgoose.common.annotation.Keep class * { *; }
-keepclassmembers @magicgoose.common.annotation.Keep class * { *; }

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/
-optimizationpasses 6
-allowaccessmodification

-keep class dclass.** { *; }
-keep interface dclass.** { *; }
-keep class elf.** { *; }
-keep interface elf.** { *; }
-keep class jlib.** { *; }
-keep interface jlib.** { *; }
-keep class jscheme.** { *; }
-keep interface jscheme.** { *; }
-keep class jsint.** { *; }
-keep interface jsint.** { *; }

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}