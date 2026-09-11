# SDK-owned runtime entry points. Ordinary JSON models use explicit org.json keys.
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers,allowoptimization class com.mparticle.internal.MParticleJSInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers,allowoptimization class com.mparticle.messaging.ProviderCloudMessage {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Optional advertising-ID and legacy Firebase methods invoked reflectively.
-keep,allowoptimization class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    public static com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep,allowoptimization class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    public java.lang.String getId();
    public boolean isLimitAdTrackingEnabled();
}
-keep,allowoptimization class com.google.firebase.iid.FirebaseInstanceId {
    public static com.google.firebase.iid.FirebaseInstanceId getInstance();
    public java.lang.String getToken(java.lang.String, java.lang.String);
}

# Exact presence probes; method calls on these optional libraries are direct.
-keep,allowoptimization class com.google.firebase.messaging.FirebaseMessaging
-keep,allowoptimization class com.android.installreferrer.api.InstallReferrerClient
-keep,allowoptimization interface com.android.installreferrer.api.InstallReferrerStateListener
-keep,allowoptimization class com.android.installreferrer.api.ReferrerDetails
-keep,allowoptimization class com.google.android.instantapps.InstantApps
-keep,allowoptimization class com.google.android.instantapps.supervisor.InstantAppsRuntime
-keep,allowoptimization class android.support.v4.app.FragmentActivity
-keepclassmembers,allowoptimization class androidx.core.app.NotificationCompat$Builder {
    public androidx.core.app.NotificationCompat$Builder setChannelId(java.lang.String);
}

# Compatibility with published kit-base versions that shipped no consumer rules.
# Names and constructors discovered by core/KitIntegrationFactory at runtime.
-keep,allowoptimization class com.mparticle.kits.KitManagerImpl {
    public <init>(android.content.Context, com.mparticle.internal.ReportingManager, com.mparticle.internal.CoreCallbacks, com.mparticle.MParticleOptions);
}
-keep,allowoptimization class com.mparticle.kits.* extends com.mparticle.kits.KitIntegration {
    public <init>();
}
# A Class token registers custom kits, so unused implementations can still shrink.
-keep,allowobfuscation,allowshrinking class * extends com.mparticle.kits.KitIntegration
-keepclassmembers class * extends com.mparticle.kits.KitIntegration {
    public <init>();
}

# InternalListenerManager identifies SDK frames by package and reads API names
# from the live stack. Preserve those names/frames without retaining unused APIs.
-keeppackagenames com.mparticle.**
-keep,allowshrinking class com.mparticle.MParticle {
    public <methods>;
}
-keep,allowshrinking class com.mparticle.internal.listeners.InternalListenerManager {
    *** onKitApiCalled(...);
}
# The stack-name fallback reflects on this interface and its reporting methods.
-keep interface com.mparticle.internal.KitManager {
    *** setLocation(...);
    *** logNetworkPerformance(...);
    *** setOptOut(...);
    *** logEvent(...);
    *** onMessageReceived(...);
    *** onPushRegistration(...);
    *** leaveBreadcrumb(...);
    *** logError(...);
    *** logException(...);
    *** logScreen(...);
}
# Match the stable interface because core obfuscates KitFrameworkWrapper before
# publishing. Both wrapper and kit-manager reporting frames must survive inlining.
-keep,allowshrinking class * implements com.mparticle.internal.KitManager {
    *** setLocation(...);
    *** logNetworkPerformance(...);
    *** setOptOut(...);
    *** logEvent(...);
    *** logCommerceEvent(...);
    *** onMessageReceived(...);
    *** onPushRegistration(...);
    *** logMPEvent(...);
    *** leaveBreadcrumb(...);
    *** logError(...);
    *** logException(...);
    *** logScreen(...);
}

# These compileOnly APIs are guarded by the presence checks above.
-dontwarn com.android.installreferrer.api.InstallReferrerClient
-dontwarn com.android.installreferrer.api.InstallReferrerClient$Builder
-dontwarn com.android.installreferrer.api.InstallReferrerStateListener
-dontwarn com.google.android.gms.tasks.OnFailureListener
-dontwarn com.google.android.gms.tasks.OnSuccessListener
-dontwarn com.google.android.gms.tasks.Task
-dontwarn com.google.android.instantapps.InstantApps
-dontwarn com.google.firebase.messaging.FirebaseMessaging
