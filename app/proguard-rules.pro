# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Paho MQTT Client
# Keep logging and persistence classes, which are often loaded via reflection.
-keep class org.eclipse.paho.client.mqttv3.logging.** { *; }
-keep class org.eclipse.paho.client.mqttv3.persist.** { *; }
-keep interface org.eclipse.paho.client.mqttv3.MqttClientPersistence

# Keep data classes and exceptions
-keep class org.eclipse.paho.client.mqttv3.MqttMessage { *; }
-keep class org.eclipse.paho.client.mqttv3.MqttConnectOptions { *; }
-keep class org.eclipse.paho.client.mqttv3.MqttException { *; }

# Keep public API interfaces and client classes
-keep public interface org.eclipse.paho.client.mqttv3.IMqttAsyncClient { *; }
-keep public interface org.eclipse.paho.client.mqttv3.IMqttClient { *; }
-keep public class org.eclipse.paho.client.mqttv3.MqttAsyncClient { *; }
-keep public class org.eclipse.paho.client.mqttv3.MqttClient { *; }
-keep public interface org.eclipse.paho.client.mqttv3.MqttCallback { *; }

-dontwarn org.eclipse.paho.client.mqttv3.**

# Remove all Android Log calls (except Errors) in the Release build
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
