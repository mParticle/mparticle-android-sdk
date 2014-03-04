package com.mparticle.networking;

import com.mparticle.MPException;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * Created by sdozor on 2/4/14.
 */
public final class MPSocketImplFactory implements SocketImplFactory {
    private Class a;
    private SocketImplFactory localFactory;

    public MPSocketImplFactory(Class paramClass) {

        this.a = paramClass;
        try {
            if (paramClass == null)
                throw new MPException();

            paramClass.newInstance();
        } catch (Exception e) {

        }

    }

    public MPSocketImplFactory(SocketImplFactory factory) {

        this.localFactory = factory;
        if ((factory = this.localFactory) == null)
            try {
                if (factory.createSocketImpl() == null) {
                    throw new MPException();
                }
                return;
            } catch (Throwable ex) {

            }

    }

    public final SocketImpl createSocketImpl() {
        SocketImpl socketImpl = null;
        if (this.localFactory != null)
            socketImpl = this.localFactory.createSocketImpl();
        else {
            try {
                socketImpl = (SocketImpl) this.a.newInstance();
            } catch (IllegalAccessException localIllegalAccessException) {
                localIllegalAccessException.printStackTrace();
            } catch (InstantiationException localInstantiationException) {
                localInstantiationException.printStackTrace();
            }
        }
        if (socketImpl != null)
            socketImpl = new MPSocketImpl(socketImpl);
        return socketImpl;
    }
}