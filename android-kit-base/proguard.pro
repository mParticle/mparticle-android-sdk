
# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-repackageclasses com.mparticle

-keep class com.mparticle.kits.MPSideloadedKit { *; }
-keep class com.mparticle.kits.MPSideloadedFilters { *; }

