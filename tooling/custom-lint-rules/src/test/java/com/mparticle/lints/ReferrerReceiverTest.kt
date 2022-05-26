package com.mparticle.lints

import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.mparticle.lints.detectors.ReferrerReceiverDetector
import org.intellij.lang.annotations.Language
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
        @Language("XML")
        val source = """<?xml version="1.0" encoding="utf-8"?>
                   <manifest package="com.mparticle.lints"
                             xmlns:android="http://schemas.android.com/apk/res/android">
                       <application>
                           <activity exported="true" android:name="com.mparticle.lints.Activity1">
                               <intent-filter>
                                   <action android:name="android.intent.action.VIEW" />
                   
                                   <category android:name="android.intent.category.HOME" />
                                   <category android:name="android.intent.category.LAUNCHER" />
                                   <category android:name="android.intent.category.DEFAULT" />
                                   <category android:name="android.intent.category.BROWSABLE" />
                               </intent-filter>
                           </activity>
                   
                           <activity exported="true" android:name="com.mparticle.lints.Activity2">
                           </activity>
                   
                           <activity exported="true" android:name="com.mparticle.lints.Activity3">
                               <intent-filter>
                                   <action android:name="android.intent.action.SEND"/>
                                   <category android:name="android.intent.category.DEFAULT"/>
                                   <data android:mimeType="text/plain"/>
                               </intent-filter>
                           </activity>
                            <receiver android:name="com.mparticle.ReferrerReceiver" android:exported="true"/>
                       </application>
                   </manifest>
                   """
        lint()
            .files(xml(FN_ANDROID_MANIFEST_XML, source))
            .run()
            .expectContains("ReferrerReceiver should be removed [" + ReferrerReceiverDetector.ISSUE.id + "]")
    }

    /**
     * Test that a manifest *without* an activity with a launcher intent reports an error.
     */
    @Throws(Exception::class)
    @Test
    fun testHasProperNoReceiver() {
        @Language("XML")
        val source = """<?xml version="1.0" encoding="utf-8" ?>
                    <manifest package="com.mparticle.lints"
                              xmlns:android="http://schemas.android.com/apk/res/android" >
                        <application>
                            <activity 
                                exported="true" 
                                android:name="com.mparticle.lints.Activity1">
                                <intent-filter>
                                    <action android:name="android.intent.action.VIEW" /> 
                     
                                    <category android:name="android.intent.category.HOME" />
                                    <category android:name="android.intent.category.LAUNCHER" /> 
                                    <category android:name="android.intent.category.DEFAULT" />
                                    <category android:name="android.intent.category.BROWSABLE" />
                                </intent-filter>
                            </activity>
                            <activity exported="true" 
                                android:name="com.mparticle.lints.Activity2">
                            </activity> 
                    
                            <activity exported="true"
                                android:name="com.mparticle.lints.Activity3"> 
                                <intent-filter>
                                    <action android:name="android.intent.action.SEND"/> 
                                    <category android:name="android.intent.category.DEFAULT"/> 
                                    <data android:mimeType="text/plain"/> 
                                </intent-filter>
                            </activity>
                        </application> 
                    </manifest>
                    """

        lint()
            .files(xml(FN_ANDROID_MANIFEST_XML, source))
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }
}
