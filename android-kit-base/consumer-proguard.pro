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
