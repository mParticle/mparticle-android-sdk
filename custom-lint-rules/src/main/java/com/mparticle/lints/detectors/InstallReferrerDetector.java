package com.mparticle.lints.detectors;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_ACTION;
import static com.android.SdkConstants.TAG_INTENT_FILTER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.mparticle.lints.ManifestConstants.ACTION_INSTALL_REFERRER;
import static com.mparticle.lints.ManifestConstants.MP_RECEIVER_NAME;

public class InstallReferrerDetector extends ResourceXmlDetector implements Detector.XmlScanner {
    public static String MESSAGE_MISSING_MP_RECEIVER = "Expecting " + ANDROID_MANIFEST_XML + " to have a " + MP_RECEIVER_NAME + " registered for INSTALL_REFERRER intent filter\n\tconsider adding:\n\n" +
            "      <receiver android:name=\"com.mparticle.ReferrerReceiver\" android:exported=\"true\">\n" +
            "          <intent-filter>\n" +
            "              <action android:name=\"com.android.vending.INSTALL_REFERRER\"/>\n" +
            "          </intent-filter>\n" +
            "      </receiver>";
    public static String MESSAGE_INTENT_FILTER_WRONG_RECEIVER = "Expecting " + TAG_RECEIVER + " with <" + TAG_INTENT_FILTER + "> containing <" + TAG_ACTION + " android:name=\"" +ACTION_INSTALL_REFERRER +
            "> tag, to be of class " + MP_RECEIVER_NAME;
    public static String MESSAGE_DUPLICATE_INTENT_FILTERS = "Expecting " + ANDROID_MANIFEST_XML +
            " to only have a single receiver with <" + TAG_INTENT_FILTER + "> containing <" +TAG_ACTION + " android:name=" + ACTION_INSTALL_REFERRER +
            "> tag.";
    public static String MESSAGE_MP_RECEIVER_MISSING_INTENT_FILTER = "It looks like <receiver android:name=" + MP_RECEIVER_NAME + "/> is properly registered, but does not have proper intent-filter.\n within the <receiver> tag in " + ANDROID_MANIFEST_XML + ", consider adding:\n " +
            "\t\t<intent-filter>\n" +
            "\t\t\t<action android:name=\"com.android.vending.INSTALL_REFERRER\"/>\n" +
            "\t\t</intent-filter>\n";


    public static final Issue ISSUE = Issue.create(
            "MParticle_Install_Referrer_Receiver",
            "MParticle ReferrerReceiver Receiver not properly registered in " + ANDROID_MANIFEST_XML,
            "MParticle ReferrerReceiver Receiver (" + MP_RECEIVER_NAME + ") needs to be registered in " + ANDROID_MANIFEST_XML + " with the IntentFilter " +
                    ACTION_INSTALL_REFERRER + ". No other receiver may be registered with this Intent Filter " +
                    "if the MParticle SDK is to function properly",
            Category.MESSAGES,
            6,
            Severity.WARNING,
            new Implementation(InstallReferrerDetector.class, Scope.MANIFEST_SCOPE));


    private int mMPReceiverCount;
    private Location mManifestLocation;

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singleton(TAG_RECEIVER);
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mManifestLocation = null;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
           if (mMPReceiverCount < 1) {
                context.report(ISSUE, mManifestLocation, MESSAGE_MISSING_MP_RECEIVER);
            }
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (context.getProject() == context.getMainProject()) {
            mManifestLocation = Location.create(context.file);
        }
    }

    @Override
    public void visitElement(XmlContext context, Element activityNode) {
        boolean isMPReceiver = false;
        boolean hasInstallReferrerIntentFilter = false;
        if (TAG_RECEIVER.equals(activityNode.getNodeName())) {
            String receiverName = activityNode.getAttributeNodeNS(ANDROID_URI, ATTR_NAME).getValue();
            if (MP_RECEIVER_NAME.equals(receiverName)) {
                isMPReceiver = true;
                mMPReceiverCount++;
            }
            for (Element activityChild : LintUtils.getChildren(activityNode)) {
                if (TAG_INTENT_FILTER.equals(activityChild.getNodeName())) {
                    for (Element intentFilterChild : LintUtils.getChildren(activityChild)) {
                        if (NODE_ACTION.equals(intentFilterChild.getNodeName())
                                && ACTION_INSTALL_REFERRER.equals(
                                intentFilterChild.getAttributeNS(ANDROID_URI, ATTR_NAME))) {
                            hasInstallReferrerIntentFilter = true;
                            if (!isMPReceiver) {
                                context.report(ISSUE, context.getLocation(activityNode), MESSAGE_INTENT_FILTER_WRONG_RECEIVER);
                            }
                        }
                    }
                }
            }
            if (isMPReceiver && !hasInstallReferrerIntentFilter) {
                context.report(ISSUE, context.getLocation(activityNode), MESSAGE_MP_RECEIVER_MISSING_INTENT_FILTER);
            }
        }
    }
}
