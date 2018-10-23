# These are the rules that are packaged with the Core SDK .aar to be used in consuming apps.

# The mParticle SDK is reading the Android Advertisment ID via reflection so it can get arround depending on
# com.google.android.gms:play-services-ads-identifier.

-keep public class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    public static com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep public final class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}