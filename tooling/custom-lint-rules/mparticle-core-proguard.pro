-dontwarn
-dontusemixedcaseclassnames
-keepattributes InnerClasses

-keep class com.mparticle.MPEvent { *; }
-keep class com.mparticle.MPEvent$* { *; }
-keep class com.mparticle.commerce.CommerceEvent { *; }
-keep class com.mparticle.commerce.CommerceEvent$* { *; }
-keep class com.mparticle.commerce.Product { *; }
-keep class com.mparticle.commerce.Product$Builder { *; }
-keep class com.mparticle.commerce.Promotion { *; }
-keep class com.mparticle.commerce.TransactionAttributes { *; }
-keep class com.mparticle.commerce.Impression { *; }
-keep class com.mparticle.MParticle$LogLevel { *; }

-keep class com.mparticle.internal.Logger { *; }
-keep class com.mparticle.internal.Logger$* { *; }

# -keep class com.mparticle.MParticle$EventType

-keep enum ** {
    *;
}

