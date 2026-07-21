  -keepclassmembers class *
  {
    @com.leanplum.annotations.* <fields>;
  }
  -keep class com.leanplum.** { *; }
  -dontwarn com.leanplum.**