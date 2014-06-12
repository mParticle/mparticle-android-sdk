package com.mparticle.audience;

/**
 * Use this callback interface to retrieve the current user's audience membership.
 */
public interface AudienceListener {
    public void onAudiencesRetrieved(AudienceMembership audienceMembership);
}
