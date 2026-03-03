# ProGuard rules for MQTT Android Service Library

# ============================
# Keep attributes needed for proper functioning
# ============================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# Keep Kotlin metadata for better interoperability
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault

# ============================
# Public API - Keep all public classes and methods
# ============================

# Main client class - primary entry point for SDK users
-keep public class info.mqtt.android.service.MqttAndroidClient {
    public protected *;
}

# Public service class
-keep public class info.mqtt.android.service.MqttService {
    public protected *;
}

# Service binder
-keep public class info.mqtt.android.service.MqttServiceBinder {
    public protected *;
}

# Token classes used in API callbacks
-keep public class info.mqtt.android.service.MqttTokenAndroid {
    public protected *;
}
-keep public class info.mqtt.android.service.MqttConnectTokenAndroid {
    public protected *;
}
-keep public class info.mqtt.android.service.MqttDeliveryTokenAndroid {
    public protected *;
}

# Public enums
-keep public enum info.mqtt.android.service.QoS {
    *;
}
-keep public enum info.mqtt.android.service.Ack {
    *;
}
-keep public enum info.mqtt.android.service.Status {
    *;
}

# Service constants
-keep public class info.mqtt.android.service.MqttServiceConstants {
    public static final *;
}

# Trace handler interface
-keep public interface info.mqtt.android.service.MqttTraceHandler {
    *;
}

# ============================
# Parcelable implementations
# ============================

# Keep Parcelable implementations and their CREATOR fields
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
    public <init>(android.os.Parcel);
}

# ParcelableMqttMessage needs special attention
-keep class info.mqtt.android.service.ParcelableMqttMessage {
    public protected *;
    public static final ** CREATOR;
}

# ============================
# Room Database
# ============================

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Room entities
-keep class info.mqtt.android.service.room.entity.** { *; }

# Room DAOs
-keep interface info.mqtt.android.service.room.MqMessageDao { *; }
-keep interface info.mqtt.android.service.room.PingDao { *; }

# Room database
-keep class info.mqtt.android.service.room.MqMessageDatabase {
    public protected *;
}

# Room TypeConverters
-keep class info.mqtt.android.service.room.Converters {
    public *;
}

# Keep Room generated classes
-keep class info.mqtt.android.service.room.**_Impl { *; }
-keep class info.mqtt.android.service.room.**$Companion { *; }

# ============================
# Eclipse Paho MQTT Client Library
# ============================

# Keep Eclipse Paho MQTT v3 classes
-keep class org.eclipse.paho.client.mqttv3.** { *; }

# Keep logging class
-keep class org.eclipse.paho.client.mqttv3.logging.JSR47Logger { *; }

# Keep persistence implementations
-keep class org.eclipse.paho.client.mqttv3.persist.** { *; }

# ============================
# Kotlin specific rules
# ============================

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep Kotlin metadata for proper reflection
-keep class kotlin.Metadata { *; }

# Keep companion objects
-keepclassmembers class ** {
    public static ** Companion;
}

# ============================
# Android Workers
# ============================

# Keep WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep ping worker
-keep class info.mqtt.android.service.ping.PingWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================
# Service Connection classes
# ============================

# Keep internal connection class as it's bound to service lifecycle
-keep class info.mqtt.android.service.MqttConnection {
    <init>(...);
}

# ============================
# Suppress D8/R8 warnings
# ============================

# Suppress D8 Kotlin metadata rewriting warnings
-dontwarn com.android.tools.r8.internal.xb4
-dontwarn org.eclipse.paho.client.mqttv3.logging.JSR47Logger

# ============================
# General optimization settings
# ============================

# Allow optimization but preserve necessary information
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release builds (optional - comment out if you want logs)
# -assumenosideeffects class timber.log.Timber {
#     public static *** v(...);
#     public static *** d(...);
#     public static *** i(...);
# }

