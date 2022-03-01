package com.mparticle.lints

import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.mparticle.lints.detectors.ReferrerReceiverDetector
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Test

class ReferrerReceiverTest : LintDetectorTest() {

    override fun getDetector() = ReferrerReceiverDetector()

    override fun getIssues() = listOf(ReferrerReceiverDetector.ISSUE)

    override fun allowMissingSdk() = true

    /**
     * Test that a manifest with com.mparticle.ReferrerReciever receiver reports an error.
     */
    @Throws(Exception::class)
    @Test
    fun testHasReferrerReceiver() {
        val result = lintProject(
            xml(
                FN_ANDROID_MANIFEST_XML,
                "" +
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<manifest package=\"com.mparticle.lints\"\n" +
                    "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                    "    <application>\n" +
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
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
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
                    ".Activity2\">\n" +
                    "        </activity>\n" +
                    "\n" +
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
                    ".Activity3\">\n" +
                    "            <intent-filter>\n" +
                    "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                    "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                    "                <data android:mimeType=\"text/plain\"/>\n" +
                    "            </intent-filter>\n" +
                    "        </activity>\n" +
                    "\n" +
                    "        <receiver android:name=\"com.mparticle.ReferrerReceiver\" android:exported=\"true\">\n" +
                    "         </receiver>\n" +
                    "    </application>\n" +
                    "</manifest>"
            )
        )
        assertThat(result)
            .contains("ReferrerReceiver should be removed [" + ReferrerReceiverDetector.ISSUE.id + "]")
    }

    /**
     * Test that a manifest *without* an activity with a launcher intent reports an error.
     */
    @Throws(Exception::class)
    @Test
    fun testHasProperNoReceiver() {
        val result = lintProject(
            xml(
                FN_ANDROID_MANIFEST_XML,
                "" +
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<manifest package=\"com.mparticle.lints\"\n" +
                    "          xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                    "    <application>\n" +
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
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
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
                    ".Activity2\">\n" +
                    "        </activity>\n" +
                    "\n" +
                    "        <activity exported=\"true\" android:name=\"com.mparticle.lints" +
                    ".Activity3\">\n" +
                    "            <intent-filter>\n" +
                    "                <action android:name=\"android.intent.action.SEND\"/>\n" +
                    "                <category android:name=\"android.intent.category.DEFAULT\"/>\n" +
                    "                <data android:mimeType=\"text/plain\"/>\n" +
                    "            </intent-filter>\n" +
                    "        </activity>\n" +
                    "\n" +
                    "         </receiver>\n" +
                    "    </application>\n" +
                    "</manifest>"
            )
        )
        assertThat(result)
            .isEqualTo(Constants.NO_WARNINGS)
    }
}
