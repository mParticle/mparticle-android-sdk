package com.mparticle.com.mparticle.audience;

import java.util.ArrayList;

/**
 This class is returned as response from a user audiences call. It contains audience ids, expiration, and a flag indicating whether it is expired.
 */
public class Audiences {
    private ArrayList<Integer> audienceIds = new ArrayList<Integer>();
    private long expiration;
    private boolean expired;
    StringBuilder list;

    /**
     The list of user audience ids
     */
    public ArrayList<Integer> getAudienceIds() {
        return audienceIds;
    }

    /**
     Contains the date the user audience will expire. If 0, it means the user audience doesn't expire
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     Flag indicating whether the user audience is expired or not
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     Returns a String with a comma separated list of user audience ids. The same user audience ids from the audiencesIds property.
     */
    @Override
    public String toString(){
        if (list == null) {
            list = new StringBuilder();

            for (Integer id : audienceIds) {
                list.append(id.toString());
                list.append(", ");
            }
            if (list.length() > 0) {
                list.delete(list.length() - 2, list.length());
            }
        }
        return list.toString();
    }

}
