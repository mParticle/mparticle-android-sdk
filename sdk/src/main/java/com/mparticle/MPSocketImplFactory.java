package com.mparticle;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

/**
 * Created by sdozor on 2/4/14.
 */
public final class MPSocketImplFactory
        implements SocketImplFactory
{
    private Class a;
    private SocketImplFactory b;


    public MPSocketImplFactory(Class paramClass)
    {

        this.a = paramClass;
        try{
        if ((paramClass = this.a) == null)
            throw new MPException();

            paramClass.newInstance();
        }catch (Exception e){

        }

    }

    public MPSocketImplFactory(SocketImplFactory paramSocketImplFactory)
    {

        this.b = paramSocketImplFactory;
        if ((paramSocketImplFactory = this.b) == null)
            try
            {
                if (paramSocketImplFactory.createSocketImpl() == null)
                {
                    throw new MPException();
                }
                return;
            }
            catch (Throwable ex)
            {

            }

    }

    public final SocketImpl createSocketImpl()
    {
        SocketImpl localObject = null;
        if (this.b != null)
            localObject = this.b.createSocketImpl();
        else
            try
            {
                localObject = (SocketImpl)this.a.newInstance();
            }
            catch (IllegalAccessException localIllegalAccessException)
            {
                localIllegalAccessException.printStackTrace();
            }
            catch (InstantiationException localInstantiationException)
            {
                localInstantiationException.printStackTrace();
            }
        if (localObject != null)
           localObject = new MPSocketImpl(localObject);
        return localObject;
    }
}