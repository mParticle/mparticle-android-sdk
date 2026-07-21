-keep class com.apptimize.** { *; }
-keepclassmembers class * extends com.apptimize.ApptimizeTest {
    <methods>;
}

-keep class android.support.v4.view.ViewPager
-keepclassmembers class android.support.v4.view.ViewPager$LayoutParams { *; }
-keep class android.support.v4.app.Fragment { *; }

-keep class com.mixpanel.android.mpmetrics.MixpanelAPI { *; }
-keep class com.google.android.gms.analytics.Tracker { *; }
-keep class com.google.analytics.tracking.android.Tracker { *; }
-keep class com.flurry.android.FlurryAgent { *; }
-keep class com.omniture.AppMeasurementBase { *; }
-keep class com.adobe.adms.measurement.ADMS_Measurement { *; }
-keep class com.adobe.mobile.Analytics { *; }
-keep class com.adobe.mobile.Config { *; }
-keep class com.localytics.android.Localytics { *; }