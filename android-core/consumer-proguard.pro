#these are the rules that are packaged with the Core SDK .aar to be used in consuming apps.
-keep class com.mparticle.** { *; }
-dontwarn com.mparticle.**
-keep class com.google.android.gms.ads.identifier.** { *; }