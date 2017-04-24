/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mparticle.lints;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.mparticle.lints.detectors.InstallReferrerDetector;

import java.util.Collections;
import java.util.List;

import static com.android.SdkConstants.FN_ANDROID_MANIFEST_XML;
import static com.mparticle.lints.detectors.InstallReferrerDetector.MESSAGE_DUPLICATE_INTENT_FILTERS;
import static com.mparticle.lints.detectors.InstallReferrerDetector.MESSAGE_INTENT_FILTER_WRONG_RECEIVER;
import static com.mparticle.lints.detectors.InstallReferrerDetector.MESSAGE_MISSING_MP_RECEIVER;
import static com.mparticle.lints.detectors.InstallReferrerDetector.MESSAGE_MP_RECEIVER_MISSING_INTENT_FILTER;
import static org.fest.assertions.api.Assertions.assertThat;


public class InstallReferrerDetectorTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new InstallReferrerDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(InstallReferrerDetector.ISSUE);
    }

    public void testCorrectReceiverMissingIntentFilter() throws Exception {
        String result = lintProject(xml(FN_ANDROID_MANIFEST_XML, "" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest package=\"com.mparticle.lints\"\n" +
                "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application>\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity1\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.VIEW\" />\n" +
                "\n" +
                "                <category android:name=\"android.intent.category.HOME\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "                <category android:name=\"android.intent.category.BROWSABLE\" " +
                "/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity2\">\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity3\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                "                <data android:mimeType=\"text/plain\"/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <receiver android:name=\"com.mparticle.ReferrerReceiver\" android:exported=\"true\"/>\n" +
                "    </application>\n" +
                "</manifest>"));
        assertThat(result)
                .contains(MESSAGE_MP_RECEIVER_MISSING_INTENT_FILTER)
                .contains(Constants.getErrorWarningMessageString(0, 1));
    }

    /**
     * Test that a manifest with an activity with a launcher intent has no warnings.
     */
    public void testDuplicateIntentFilters() throws Exception {
        String result = lintProject(xml(FN_ANDROID_MANIFEST_XML, "" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest package=\"com.mparticle.lints\"\n" +
                "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application>\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity1\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.VIEW\" />\n" +
                "\n" +
                "                <category android:name=\"android.intent.category.HOME\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "                <category android:name=\"android.intent.category.BROWSABLE\" " +
                "/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity2\">\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity3\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                "                <data android:mimeType=\"text/plain\"/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <receiver android:name=\"com.mparticle.ReferrerReceiver\" android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "               <action android:name=\"com.android.vending.INSTALL_REFERRER\"/>\n" +
                "            </intent-filter>\n" +
                "         </receiver>" +
                "         <receiver android:name=\"com.example.SomeOtherReceiver\" android:exported=\"true\">" +
                "            <intent-filter>\n" +
                "               <action android:name=\"com.android.vending.INSTALL_REFERRER\"/>\n" +
                "            </intent-filter>\n" +
                "         </receiver>\n" +
                "    </application>\n" +
                "</manifest>"));
        assertThat(result)
                .contains(MESSAGE_INTENT_FILTER_WRONG_RECEIVER)
                .contains(Constants.getErrorWarningMessageString(0, 1));
    }

    /**
     * Test that a manifest <em>without</em> an activity with a launcher intent reports an error.
     */
    public void testHasProperReceiver() throws Exception {
        String result = lintProject(xml(FN_ANDROID_MANIFEST_XML, "" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest package=\"com.mparticle.lints\"\n" +
                "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application>\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity1\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.VIEW\" />\n" +
                "\n" +
                "                <category android:name=\"android.intent.category.HOME\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "                <category android:name=\"android.intent.category.BROWSABLE\" " +
                "/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity2\">\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity3\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                "                <data android:mimeType=\"text/plain\"/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <receiver android:name=\"com.mparticle.ReferrerReceiver\" android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "               <action android:name=\"com.android.vending.INSTALL_REFERRER\"/>\n" +
                "            </intent-filter>\n" +
                "         </receiver>\n" +
                "    </application>\n" +
                "</manifest>"));
        assertThat(result)
                .isEqualTo(Constants.NO_WARNINGS);
    }

    /**
     * Test that a manifest without an <code>&lt;application&gt;</code> tag reports an error.
     */
    public void testMissingReceiverAndIntentFilter() throws Exception {
        String result = lintProject(xml(FN_ANDROID_MANIFEST_XML, "" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest package=\"com.mparticle.lints\"\n" +
                "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application>\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity1\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.VIEW\" />\n" +
                "\n" +
                "                <category android:name=\"android.intent.category.HOME\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "                <category android:name=\"android.intent.category.BROWSABLE\" " +
                "/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity2\">\n" +
                "        </activity>\n" +
                "\n" +
                "        <activity android:name=\"com.mparticle.lints" +
                ".Activity3\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                "                <data android:mimeType=\"text/plain\"/>\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "\n" +
                "        <receiver android:name=\"com.example.Receiver\" android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "               <action android:name=\"com.some.other.INTENT_FILTER\"/>\n" +
                "            </intent-filter>\n" +
                "         </receiver>\n" +
                "    </application>\n" +
                "</manifest>"));
        assertThat(result)
                .contains(MESSAGE_MISSING_MP_RECEIVER)
                .contains(Constants.getErrorWarningMessageString(0, 1));
    }

//    private String standardizeError(String error, int errors, int warnings) {
//        return "AndroidManifest.xml: Error: " + error + " [" + ISSUE.getId() + "]\n" +
//                Constants.getErrorWarningMessageString(errors, warnings) + "\n";
//    }
}
