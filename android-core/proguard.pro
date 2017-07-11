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

-keep class com.mparticle.MPEvent { *; }
-keep class com.mparticle.messaging.InstanceIdService { *; }
-keep class com.mparticle.MPEvent$* { *; }
-keep, includedescriptorclasses class com.mparticle.MParticle { *; }
-keep class com.mparticle.MParticle$* { *; }
-keep class com.mparticle.BuildConfig { *; }
-keep class com.mparticle.MPReceiver { *; }
-keep class com.mparticle.MPService { *; }
-keep class com.mparticle.MParticleOptions { *;}
-keep class com.mparticle.MParticleOptions$* { *; }
-keep class com.mparticle.DeepLinkError { *; }
-keep class com.mparticle.DeepLinkListener { *; }
-keep class com.mparticle.DeepLinkResult { *; }
-keep class com.mparticle.TaskSuccessListener { *; }
-keep class com.mparticle.TaskFailureListener { *; }
-keep, includedescriptorclasses class com.mparticle.MParticleTask { *; }
-keep, includedescriptorclasses class com.mparticle.internal.AppStateManager { *; }
-keep class com.mparticle.internal.ApplicationContextWrapper { *; }
-keep class com.mparticle.internal.KitManager { *; }
-keep class com.mparticle.internal.KitFrameworkWrapper { *; }
-keep class com.mparticle.internal.ConfigManager { *; }
-keep class com.mparticle.internal.ReportingManager { *; }
-keep class com.mparticle.internal.JsonReportingMessage { *; }
-keep class com.mparticle.internal.MPUtility { *; }
-keep class com.mparticle.internal.MPUtility$* { *; }
-keep class com.mparticle.internal.PushRegistrationHelper { *; }
-keep class com.mparticle.internal.PushRegistrationHelper* { *; }
-keep class com.mparticle.ReferrerReceiver { *; }
-keep class com.mparticle.kits.ForeseeKit { *; }
-keep class com.mparticle.UserAttributeListener { *; }
-keep class com.mparticle.internal.MParticleJSInterface { *; }
-keep class com.mparticle.internal.Logger { *; }
-keep, includedescriptorclasses class com.mparticle.identity.IdentityApi { *; }
-keep, includedescriptorclasses class com.mparticle.identity.IdentityApiRequest { *; }
-keep, includedescriptorclasses class com.mparticle.identity.IdentityApiRequest$* { *; }
-keep, includedescriptorclasses class com.mparticle.identity.IdentityApiResult { *; }
-keep, includedescriptorclasses class com.mparticle.identity.MParticleUser { *; }
-keep, includedescriptorclasses class com.mparticle.identity.IdentityStateListener { *; }
-keep, includedescriptorclasses public class com.mparticle.identity.* { *; }

-keep public class com.mparticle.activity.* {
    *;
}

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

-keep class com.mparticle.internal.PushRegistrationHelper