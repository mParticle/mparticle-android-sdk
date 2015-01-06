package com.mparticle;

/**
 * Helper class to listen for different events related to receiving and interacting with push notifications
 * received from the Google Cloud Messaging service.
 *
 * Note that listening for MParticle push events and broadcasts requires that you've properly registered the correct
 * permission in your AndroidManifest.xml. This is done so that other apps can not intercept potentially sensitive data sent
 * via a GCM message:
 *
 * <pre>
 * {@code
 *  <permission
 *      android:name="YOURPACKAGENAME.mparticle.permission.NOTIFICATIONS"
 *      android:protectionLevel="signature" />
 * <uses-permission android:name="YOURPACKAGENAME.mparticle.permission.NOTIFICATIONS" />
 * }</pre>
 */
public final class MParticlePushUtility {




}
