package com.mparticle.lints

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.mparticle.lints.detectors.DataplanDetector
import com.mparticle.lints.detectors.GradleBuildDetector
import com.mparticle.lints.detectors.MpApiDetectorKt
import com.mparticle.lints.detectors.ReferrerReceiverDetector

/**
 * The list of issues that will be checked when running <code>lint</code>.
 */
@SuppressWarnings("unused")
class MParticleIssueRegistry : IssueRegistry() {
    override val issues = listOf(
        GradleBuildDetector.ISSUE,
        MpApiDetectorKt.ISSUE,
        ReferrerReceiverDetector.ISSUE,
        DataplanDetector.ISSUE,
        DataplanDetector.NODE_MISSING,
        DataplanDetector.NO_DATA_PLAN
    )

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "mParticle",
        identifier = "android-core",
        feedbackUrl = "https://github.com/mParticle/mparticle-android-sdk"
    )
}
