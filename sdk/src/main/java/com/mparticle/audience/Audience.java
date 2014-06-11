package com.mparticle.audience;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;

/**
 * Created by sdozor on 4/9/14.
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

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String[] getEndpoints(){
        if (endpoints != null) {
            return endpoints;
        }else{
            return new String[]{};
        }
    }

    @Override
    public String toString() {
        return "Audience ID:  " + id + ", " +
               "Name: " + name + ", " +
               "Endpoints: " + ((endpoints != null && endpoints.length > 0) ? Arrays.toString(endpoints) : "None specified");
    }
}
