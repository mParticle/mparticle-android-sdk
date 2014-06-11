package com.mparticle.audience;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;

/**
 * This class represents a single Audience of which one or more users may be a member.
 */
public class Audience {
    int id;
    String name;
    String[] endpoints;

    public Audience(int id, String name, String endpointBlob) {
        this.id = id;
        this.name = name;
        try {
            JSONArray endpointJson = new JSONArray(endpointBlob);
            endpoints = new String[endpointJson.length()];
            for (int i = 0; i < endpointJson.length(); i++){
                endpoints[i] = endpointJson.getString(i);
            }
        }catch (JSONException jse){

        }

    }

    /**
     * Retrieve the unique audience ID.
     *
     * @return an integer ID
     */
    public int getId(){
        return id;
    }

    /**
     * Retrieve the display name for this Audience, configured via the mParticle web console.
     *
     * @return
     */
    public String getName(){
        return name;
    }

    /**
     * Retrieve the endpoint IDs to which this Audience is configured to forward
     *
     * @return an array of IDs
     */
    public String[] getEndpoints(){
        if (endpoints != null) {
            return endpoints;
        }else{
            return new String[]{};
        }
    }

    /**
     * Retrieve a readable summary of this Audience
     *
     * @return Audience summary
     */
    @Override
    public String toString() {
        return "Audience ID:  " + id + ", " +
               "Name: " + name + ", " +
               "Endpoints: " + ((endpoints != null && endpoints.length > 0) ? Arrays.toString(endpoints) : "None specified");
    }
}
