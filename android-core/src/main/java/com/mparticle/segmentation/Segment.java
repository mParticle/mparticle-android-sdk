package com.mparticle.segmentation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;

/**
 * This class represents a single Segment of which one or more users may be a member.
 */
public class Segment {
    int id;
    String name;
    String[] endpoints;

    public Segment(int id, @NonNull String name, @NonNull String endpointBlob) {
        this.id = id;
        this.name = name;
        try {
            JSONArray endpointJson = new JSONArray(endpointBlob);
            endpoints = new String[endpointJson.length()];
            for (int i = 0; i < endpointJson.length(); i++) {
                endpoints[i] = endpointJson.getString(i);
            }
        } catch (JSONException jse) {

        }

    }

    /**
     * Retrieve the unique segment ID.
     *
     * @return an integer ID
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieve the display name for this Segment, configured via the mParticle web console.
     *
     * @return
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Retrieve the endpoint IDs to which this Segment is configured to forward.
     *
     * @return an array of IDs
     */
    @NonNull
    public String[] getEndpoints() {
        if (endpoints != null) {
            return endpoints;
        } else {
            return new String[]{};
        }
    }

    /**
     * Retrieve a readable summary of this Segment.
     *
     * @return Segment summary
     */
    @Override
    @NonNull
    public String toString() {
        return "Segment ID:  " + id + ", " +
                "Name: " + name + ", " +
                "Endpoints: " + ((endpoints != null && endpoints.length > 0) ? Arrays.toString(endpoints) : "None specified.");
    }
}
