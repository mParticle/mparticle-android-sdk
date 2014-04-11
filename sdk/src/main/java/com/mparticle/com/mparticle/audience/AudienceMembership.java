package com.mparticle.com.mparticle.audience;

import java.util.ArrayList;

/**
 This class is returned as response from a user audiences call. It contains audience ids, expiration, and a flag indicating whether it is expired.
 */
public class AudienceMembership {
    private ArrayList<Audience> audiences;
    private boolean expired;
    StringBuilder list;

    public AudienceMembership(ArrayList<Audience> ids) {
        super();
        audiences = ids;
    }

    /**
     The list of user audience ids
     */
    public ArrayList<Audience> getAudiences() {
        return audiences;
    }

    /**
     Returns a String with a comma separated list of user audience ids.
     */
    @Override
    public String toString(){
        if (list == null) {
            list = new StringBuilder();

            for (Audience audience : audiences) {
                list.append(audience.getId());
                list.append(", ");
            }
            if (list.length() > 0) {
                list.delete(list.length() - 2, list.length());
            }
        }
        return list.toString();
    }

    public String getCommaSeparatedIds() {
        return toString();
    }

}
