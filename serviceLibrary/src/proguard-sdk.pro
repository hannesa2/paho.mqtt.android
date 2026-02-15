-keep class info.mqtt.android.service.** { *;}
-keep class org.eclipse.paho.client.mqttv3.logging.JSR47Logger { *; }

# Suppress D8 Kotlin metadata rewriting warnings for Room DAOs
-dontwarn com.android.tools.r8.internal.xb4
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses


