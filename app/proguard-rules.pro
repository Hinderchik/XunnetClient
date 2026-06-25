# ProGuard rules for Xunnet
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Hilt
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Keep Room entities
-keep class dev.xunnet.client.core.data.local.entity.** { *; }

# Keep models
-keep class dev.xunnet.client.core.domain.model.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep gRPC
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }

# Keep Retrofit
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
