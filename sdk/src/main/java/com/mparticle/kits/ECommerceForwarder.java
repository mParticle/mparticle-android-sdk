package com.mparticle.kits;

import com.mparticle.commerce.CommerceEvent;

import java.util.List;

/**
 * Created by sdozor on 7/29/15.
 */
public interface ECommerceForwarder extends ClientSideForwarder{
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception;
}
