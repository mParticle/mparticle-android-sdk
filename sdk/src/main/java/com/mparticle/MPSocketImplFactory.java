package com.mparticle;

import android.util.Log;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * Created by sdozor on 2/4/14.
 */
public final class MPSocketImplFactory implements SocketImplFactory {
    private Class socketClass;

    public MPSocketImplFactory(Class paramClass) throws IllegalAccessException, InstantiationException {
        socketClass = paramClass;
        //test creating a new instance so in the case that it fails,
        //we don't completely break the users' network connection
        this.socketClass.newInstance();
    }

    public final SocketImpl createSocketImpl() {
        try {
            SocketImpl socketImpl = (SocketImpl) this.socketClass.newInstance();
            if (MParticle.getInstance().mConfigManager.isNetworkPerformanceEnabled()){
                return new MPSocketImpl(socketImpl);
            }else{
                return socketImpl;
            }
        } catch (Exception ex) {
            if (MParticle.getInstance().getDebugMode()){
                Log.w(Constants.LOG_TAG, "Failed to create new Socket");
            }
        }
        return null;
    }
}