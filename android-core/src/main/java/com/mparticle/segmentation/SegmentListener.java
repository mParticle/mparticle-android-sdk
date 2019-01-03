package com.mparticle.segmentation;

import android.support.annotation.Nullable;

/**
 * Use this callback interface to retrieve the current user's segment membership.
 */
public interface SegmentListener {
    void onSegmentsRetrieved(@Nullable SegmentMembership segmentMembership);
}
