package com.mparticle.internal.embedded;

import com.mparticle.MPProduct;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.ReportingMessage;

import java.util.List;

/**
 * Created by sdozor on 7/29/15.
 */
interface ECommerceForwarder extends ClientSideForwarder{
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception;


}
