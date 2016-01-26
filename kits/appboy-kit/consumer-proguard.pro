# These are the proguard rules specified by the Appboy SDK's documentation

-dontwarn com.amazon.device.messaging.**
-dontwarn bo.app.**
-dontwarn com.appboy.ui.**
-dontwarn com.google.android.gms.**
-keep class bo.app.** { *; }
-keep class com.appboy.** { *; }