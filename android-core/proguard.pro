#these are the rules that mParticle used when compiling the core library

-optimizations !code/allocation/variable

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep class com.mparticle.MPEvent { *; }
-keep class com.mparticle.messaging.InstanceIdService { *; }
-keep class com.mparticle.MPEvent$* { *; }
-keep class com.mparticle.kits.Constants { *; }
-keep class com.mparticle.kits.Constants* { *; }
-keep class com.mparticle.MParticle { *; }
-keep class com.mparticle.MParticle$* { *; }
-keep class com.mparticle.BuildConfig { *; }
-keep class com.mparticle.MPReceiver { *; }
-keep class com.mparticle.MPService { *; }
-keep class com.mparticle.DeepLinkError { *; }
-keep class com.mparticle.DeepLinkListener { *; }
-keep class com.mparticle.DeepLinkResult { *; }
-keep class com.mparticle.internal.AppStateManager { *; }
-keep class com.mparticle.internal.KitManager { *; }
-keep class com.mparticle.internal.ConfigManager { *; }
-keep class com.mparticle.internal.ReportingManager { *; }
-keep class com.mparticle.internal.ReportingMessage { *; }
-keep class com.mparticle.internal.MPUtility { *; }
-keep class com.mparticle.internal.MPUtility$* { *; }
-keep class com.mparticle.internal.PushRegistrationHelper { *; }
-keep class com.mparticle.internal.PushRegistrationHelper* { *; }
-keep class com.mparticle.ReferrerReceiver { *; }
-keep class com.mparticle.internal.CommerceEventUtil { *; }
-keep class com.mparticle.kits.ForeseeKit { *; }


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