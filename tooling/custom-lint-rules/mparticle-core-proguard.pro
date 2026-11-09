-dontwarn
-dontusemixedcaseclassnames
# Keep each local variable in the slot the compiler gave it. This config names no
# library jars, so java.lang.Object is unknown to ProGuard. When the variable
# optimization reuses one slot for two unrelated classes (Impression and Product in
# MPCommerceMessage's Kotlin loops), the preverifier cannot merge their types and
# stops with IncompleteClassHierarchyException.
-optimizations !code/allocation/variable
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

