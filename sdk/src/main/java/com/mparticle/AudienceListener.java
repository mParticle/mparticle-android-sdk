package com.mparticle;

import com.mparticle.audience.AudienceMembership;

/**
 * Created by sdozor on 4/7/14.
 */
public interface AudienceListener {
    public void onAudiencesRetrieved(AudienceMembership audienceMembership);
}
