# These are the proguard rules specified by the Wootric SDK's documentation

-keep class com.wootric.** { *; }
-keep class retrofit.** { *; }
-keepclassmembernames interface * {
    @retrofit.http.* <methods>;
}