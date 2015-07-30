package com.mparticle.internal.embedded;

import com.mparticle.commerce.CommerceEvent;

/**
 * Created by sdozor on 7/29/15.
 */
interface ECommerceForwarder extends ClientSideForwarder{
    public void logEvent(CommerceEvent event) throws Exception;
}
