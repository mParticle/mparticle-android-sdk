package com.mparticle.segmentation;

/**
 * Use this callback interface to retrieve the current user's segment membership.
 */
public interface SegmentListener {
    public void onSegmentsRetrieved(SegmentMembership segmentMembership);
}
