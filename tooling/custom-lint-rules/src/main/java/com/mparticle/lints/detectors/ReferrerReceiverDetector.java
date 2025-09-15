package com.mparticle.lints.detectors;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_RECEIVER;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;


public class ReferrerReceiverDetector extends ResourceXmlDetector implements Detector.XmlScanner {
    public static final String ACTION_INSTALL_REFERRER = "com.android.vending.INSTALL_REFERRER";
    public static final String MP_RECEIVER_NAME = "com.mparticle.ReferrerReceiver";

    public static final Issue ISSUE = Issue.create(
            "MParticleInstallRefReceiver",
            "com.mparticle.ReferrerReceiver should no longer be used",
            "MParticle ReferrerReceiver should no longer be registered in " + ANDROID_MANIFEST_XML + ". In order to receive InstallReferrer data, add the following dependency to your build.gradle\n" +
                    "\n" +
                    "dependencies {\n" +
                    "\timplementation 'com.android.installreferrer:installreferrer:1+'" +
                    "\n}",
            Category.MESSAGES,
            10,
            Severity.ERROR,
            new Implementation(ReferrerReceiverDetector.class, Scope.MANIFEST_AND_RESOURCE_SCOPE,
                    Scope.MANIFEST_SCOPE));

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singleton(TAG_RECEIVER);
    }

    @Override
    public void visitElement(XmlContext context, Element activityNode) {
        if (TAG_RECEIVER.equals(activityNode.getNodeName())) {
            String receiverName = activityNode.getAttributeNodeNS(ANDROID_URI, ATTR_NAME).getValue();
            if (MP_RECEIVER_NAME.equals(receiverName)) {
                context.report(ISSUE, context.getLocation(activityNode), "ReferrerReceiver should be removed");
            }
        }
    }
}