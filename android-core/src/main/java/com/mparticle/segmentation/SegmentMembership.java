package com.mparticle.segmentation;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 This class is returned as response from a user segments call. It contains segment ids, expiration, and a flag indicating whether it is expired.
 */
public class SegmentMembership {
    private ArrayList<Segment> segments;
    StringBuilder list;

    public SegmentMembership(@NonNull ArrayList<Segment> ids) {
        super();
        segments = ids;
    }

    /**
     The list of user segment ids
     */
    @NonNull
    public ArrayList<Segment> getSegments() {
        return segments;
    }

    /**
     Returns a String with a comma separated list of user segment ids.
     */
    @Override
    @NonNull
    public String toString(){
        return getCommaSeparatedIds();
    }

    /**
     Returns a String with a comma separated list of user segment ids.
     */
    @NonNull
    public String getCommaSeparatedIds() {
        if (list == null) {
            list = new StringBuilder();

            for (Segment segment : segments) {
                list.append(segment.getId());
                list.append(", ");
            }
            if (list.length() > 0) {
                list.delete(list.length() - 2, list.length());
            }
        }
        return list.toString();
    }

}
