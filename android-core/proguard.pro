#these are the rules that mParticle used when compiling the core library
# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
-dontoptimize
#-dontpreverify

# Note that if you want to enable optimization, you cannot just
# include optimization flags in your own project configuration file;
# instead you will need to point to the
# "proguard-android-optimize.txt" file instead of this one from your
# project.properties file.

-keepattributes *Annotation*
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

# Understand the @Keep support annotation.
-keep class android.support.annotation.Keep

-keep @android.support.annotation.Keep class * {*;}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}

-optimizations !code/allocation/variable

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-repackageclasses com.mparticle

-keep class com.mparticle.MPEvent$* { *; }
-keep class com.mparticle.MParticle { *; }
-keep class com.mparticle.MParticle$EventType { *; }
-keep class com.mparticle.MParticle$InstallType { *; }
-keep class com.mparticle.MParticle$IdentityType { *; }
-keep class com.mparticle.MParticle$Environment { *; }
-keep class com.mparticle.MParticle$LogLevel { *; }
-keep class com.mparticle.MParticle$Builder { *; }
-keep class com.mparticle.MParticle$ServiceProviders { *; }
-keep class com.mparticle.MParticle$UserAttributes { *; }
-keep class com.mparticle.MParticle$ResetListener { *; }
-keep class com.mparticle.MParticle$OperatingSystem { *; }
-keep class com.mparticle.MParticle$MpRoktEventCallback { *; }
-keep class com.mparticle.MParticle$UnloadReasons { *; }
-keep class com.mparticle.MParticle$Rokt { *; }
-keep class com.mparticle.WrapperSdk { *; }
-keep class com.mparticle.WrapperSdkVersion { *; }

-keep class com.mparticle.Session { *; }
-keep class com.mparticle.MPEvent { *; }
-keep class com.mparticle.BuildConfig { *; }
-keep class com.mparticle.MPReceiver { *; }
-keep class com.mparticle.MPService { *; }
-keep class com.mparticle.MParticleOptions { *;}
-keep class com.mparticle.MParticleOptions$* { *; }
-keep class com.mparticle.AttributionError { *; }
-keep class com.mparticle.AttributionListener { *; }
-keep class com.mparticle.AttributionResult { *; }
-keep class com.mparticle.MParticleTask { *; }
-keep class com.mparticle.UserAttributeListener { *; }
-keep class com.mparticle.TypedUserAttributeListener { *; }
-keep class com.mparticle.UserAttributeListenerType { *; }
-keep class com.mparticle.BaseEvent { *; }
-keep class com.mparticle.BaseEvent$Type { *; }
-keep class com.mparticle.BaseEvent$MessageType { *; }
-keep class com.mparticle.Configuration { *; }

-keep class com.mparticle.consent.ConsentState { *; }
-keep class com.mparticle.consent.ConsentState$Builder { *; }
-keep class com.mparticle.consent.GDPRConsent { *; }
-keep class com.mparticle.consent.GDPRConsent$Builder { *; }
-keep class com.mparticle.consent.CCPAConsent { *; }
-keep class com.mparticle.consent.CCPAConsent$Builder {*;}


-keep class com.mparticle.internal.SideloadedKit { *; }

-keep class com.mparticle.internal.KitManager { *; }
-keep class com.mparticle.internal.KitManager$* { *; }
-keep class com.mparticle.internal.CoreCallbacks { *; }
-keep class com.mparticle.internal.CoreCallbacks$KitListener { *; }
-keep class com.mparticle.internal.ReportingManager { *; }
-keep class com.mparticle.internal.JsonReportingMessage { *; }
-keep class com.mparticle.internal.MPUtility { *; }
-keep class com.mparticle.internal.MPUtility$* { *; }
-keep class com.mparticle.UserAttributeListener { *; }
-keep class com.mparticle.internal.PushRegistrationHelper { *; }
-keep class com.mparticle.internal.PushRegistrationHelper$PushRegistration { *; }
-keep class com.mparticle.internal.MParticleJSInterface { *; }
-keep class com.mparticle.internal.Logger { *; }
-keep class com.mparticle.internal.Logger$* { *; }
-keep class com.mparticle.internal.KitsLoadedCallback { *; }
-keep class com.mparticle.internal.listeners.InternalListenerManager { *; }

-keep class com.mparticle.identity.IdentityApi { *; }
-keep class com.mparticle.identity.IdentityApiRequest { *; }
-keep class com.mparticle.identity.IdentityApiRequest$* { *; }
-keep class com.mparticle.identity.IdentityApiResult { *; }
-keep class com.mparticle.identity.MParticleUser { *; }
-keep class com.mparticle.identity.IdentityStateListener { *; }
-keep class com.mparticle.identity.UserAliasHandler { *; }
-keep class com.mparticle.identity.IdentityHttpResponse { *; }
-keep class com.mparticle.identity.IdentityHttpResponse$Error { *; }
-keep class com.mparticle.identity.TaskSuccessListener { *; }
-keep class com.mparticle.identity.TaskFailureListener { *; }
-keep class com.mparticle.identity.BaseIdentityTask { *; }
-keep class com.mparticle.identity.AliasRequest { *; }
-keep class com.mparticle.identity.AliasRequest$Builder { *; }
-keep class com.mparticle.identity.AliasResponse { *; }

-keep class com.mparticle.messaging.InstanceIdService { *; }

-keep class com.mparticle.networking.NetworkOptions { *;}
-keep class com.mparticle.networking.NetworkOptions$Builder { *; }
-keep class com.mparticle.networking.DomainMapping { *; }
-keep class com.mparticle.networking.DomainMapping$Builder { *; }
-keep class com.mparticle.networking.Certificate { *; }
-keep class com.mparticle.SdkListener { *; }
-keep class com.mparticle.SdkListener$* { *; }
-keep class com.mparticle.internal.listeners.GraphListener { *; }

-keep class com.mparticle.internal.InternalSession { *; }

-keep public class com.mparticle.messaging.* {
     *;
}

-keep public class com.mparticle.segmentation.* {
    *;
}

-keep public class com.mparticle.media.* {
    *;
}

-keep public class com.mparticle.commerce.* {
    *;
}

-keep public class com.mparticle.kits.* {
    *;
}

-keep public class com.mparticle.rokt.* {
    *;
}
-keep public class com.mparticle.audience.* {
    *;
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}