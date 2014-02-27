package com.mparticle.networking;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 * Created by sdozor on 2/5/14.
 */
public class MPSocketImpl extends SocketImpl {
    private static Field a;
    private static Field b;
    private static Field c;
    private static Field d;
    private SocketImpl localSocket;
    private static Method[] e = new Method[20];
    private InputStream m;

    public static void acc(AccessibleObject[] paramArrayOfAccessibleObject)
    {
        for (int i = 0; i < paramArrayOfAccessibleObject.length; i++)
        {
            AccessibleObject localAccessibleObject;
            if ((localAccessibleObject = paramArrayOfAccessibleObject[i]) != null)
                localAccessibleObject.setAccessible(true);
        }
    }


    public MPSocketImpl(SocketImpl localObject) {
        localSocket = localObject;

        try
        {
            Class localSocketImpl = SocketImpl.class;
            a = localSocketImpl.getDeclaredField("address");
            b = localSocketImpl.getDeclaredField("fd");
            c = localSocketImpl.getDeclaredField("localport");
            d = localSocketImpl.getDeclaredField("port");
            AccessibleObject[] arrayOfAccessibleObject = { b, c, d };
            Field localField;
            if ((localField = a) != null)
                localField.setAccessible(true);
            if (arrayOfAccessibleObject.length > 0)
                acc(arrayOfAccessibleObject);
            e[0] = localSocketImpl.getDeclaredMethod("accept", new Class[]{SocketImpl.class});
            e[1] = localSocketImpl.getDeclaredMethod("available", new Class[0]);
            e[2] = localSocketImpl.getDeclaredMethod("bind", new Class[] { InetAddress.class, Integer.TYPE });
            e[3] = localSocketImpl.getDeclaredMethod("close", new Class[0]);
            e[4] = localSocketImpl.getDeclaredMethod("connect", new Class[] { InetAddress.class, Integer.TYPE });
            e[5] = localSocketImpl.getDeclaredMethod("connect", new Class[] { SocketAddress.class, Integer.TYPE });
            e[6] = localSocketImpl.getDeclaredMethod("connect", new Class[] { String.class, Integer.TYPE });
            e[7] = localSocketImpl.getDeclaredMethod("create", new Class[] { Boolean.TYPE });
            e[8] = localSocketImpl.getDeclaredMethod("getFileDescriptor", new Class[0]);
            e[9] = localSocketImpl.getDeclaredMethod("getInetAddress", new Class[0]);
            e[10] = localSocketImpl.getDeclaredMethod("getInputStream", new Class[0]);
            e[11] = localSocketImpl.getDeclaredMethod("getLocalPort", new Class[0]);
            e[12] = localSocketImpl.getDeclaredMethod("getOutputStream", new Class[0]);
            e[13] = localSocketImpl.getDeclaredMethod("getPort", new Class[0]);
            e[14] = localSocketImpl.getDeclaredMethod("listen", new Class[] { Integer.TYPE });
            e[15] = localSocketImpl.getDeclaredMethod("sendUrgentData", new Class[] { Integer.TYPE });
            e[16] = localSocketImpl.getDeclaredMethod("setPerformancePreferences", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
            e[17] = localSocketImpl.getDeclaredMethod("shutdownInput", new Class[0]);
            e[18] = localSocketImpl.getDeclaredMethod("shutdownOutput", new Class[0]);
            e[19] = localSocketImpl.getDeclaredMethod("supportsUrgentData", new Class[0]);
            acc(e);
            f = true;
        }
        catch (SecurityException localSecurityException)
        {
            f = false;
            //   g = cd.P;
        }
        catch (NoSuchMethodException localNoSuchMethodException)
        {
            f = false;
            // g = cd.Q;
            int n = 20;
            for (int i1 = 0; i1 < 20; i1++)
                if (e[i1] == null)
                {
                    n = i1;
                    break;
                }

        }
        catch (NoSuchFieldException localNoSuchFieldException)
        {
            f = false;
            //  g = cd.R;

        }
        catch (Throwable localThrowable)
        {
            f = false;
            //  g = cd.S;
        }

}



    private Object a(int paramInt, Object[] paramArrayOfObject) throws Exception
    {
        MPSocketImpl localMPSocketImpl = this;
        try
        {
            a.set(localMPSocketImpl.localSocket, localMPSocketImpl.address);
            b.set(localMPSocketImpl.localSocket, localMPSocketImpl.fd);
            c.setInt(localMPSocketImpl.localSocket, localMPSocketImpl.localport);
            d.setInt(localMPSocketImpl.localSocket, localMPSocketImpl.port);
        }
        catch (Exception e)
        {
           // Log.d(Constants.LOG_TAG, e.toString());
            throw new Exception("fuckers");
        }
        try
        {
            return e[paramInt].invoke(this.localSocket, paramArrayOfObject);

        }
        catch (Exception localException)
        {
            throw new Exception("fucksauce");
        }
        finally
        {
            f();
        }
    }

    private void f() throws Exception
    {
        try
        {
            this.address = ((InetAddress)a.get(this.localSocket));
            this.fd = ((FileDescriptor)b.get(this.localSocket));
            this.localport = c.getInt(this.localSocket);
            this.port = d.getInt(this.localSocket);
            return;
        }
        catch (Exception e){
            throw new Exception("fuckkk" + e.toString());
        }
    }

    private Object c(int paramInt, Object[] paramArrayOfObject) throws IOException
    {
        try
        {
            return a(paramInt, paramArrayOfObject);
        }
        catch (IOException iox){
            throw iox;
        }
        catch (Exception ex)
        {
          //  Log.d(Constants.LOG_TAG, ex.toString());
            return null;
        }
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        try{
            c(0, new Object[] { s });
        }catch(IOException e){
            throw e;
        }catch (Exception e){
          //  Log.d(Constants.LOG_TAG, e.toString());
        }
    }

    @Override
    protected int available() throws IOException {
        Integer localInteger;
        if ((localInteger = (Integer)c(1, new Object[0])) == null){
           //  Log.d(Constants.LOG_TAG, "fuooocker");
        }

        return localInteger.intValue();
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        c(2, new Object[] { host, Integer.valueOf(port) });
    }

    @Override
    protected void close() throws IOException {
        c(3, new Object[0]);
        try
        {
          //  if (this.m != null)
            //    this.m.d();
            return;
        }
        catch (ThreadDeath localThreadDeath)
        {
            throw localThreadDeath;
        }
        catch (Throwable localThrowable)
        {
            //dm.a(localThrowable);
        }
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        try
        {
            c(6, new Object[] { host, Integer.valueOf(port) });
           // Log.d(Constants.LOG_TAG, "Connecting to host: " + host);
            return;
        }
        catch (IOException localIOException)
        {
            try
            {
                int n = port;
                String str = host;
               /* port = localIOException;
                host = this;
                if (str != null)
                {
                    MeasuredRequest localb;
                    (localb = host.a(false)).e();
                    localb.a(str);
                    localb.a(n);
                    localb.g = cf.a(port);
                    host.i.a(localb, MeasuredRequest.a.i);
                }*/
            }
            catch (ThreadDeath localThreadDeath)
            {
                throw localThreadDeath;
            }
            catch (Throwable localThrowable)
            {
               // dm.a(localThrowable);
            }
            throw localIOException;
        }

    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
            try
            {
                c(4, new Object[] { address, Integer.valueOf(port) });
                if (address != null)
                   // Log.d(Constants.LOG_TAG, "Connecting to host: " + address.toString());
                return;
            }
            catch (IOException localIOException)
            {
                try
                {
                    int n = port;
                    InetAddress localInetAddress = address;
                   /* port = localIOException;
                    address = this;
                    if (localInetAddress != null)
                    {
                        MeasuredRequest localb;
                        (localb = address.a(false)).e();
                        localb.a(localInetAddress);
                        localb.a(n);
                        localb.g = cf.a(port);
                        address.i.a(localb, MeasuredRequest.a.i);
                    }*/
                }
                catch (ThreadDeath localThreadDeath)
                {
                    throw localThreadDeath;
                }
                catch (Throwable localThrowable)
                {
                   // dm.a(localThrowable);
                }
                throw localIOException;
            }
    }

    @Override
    protected void create(boolean isStreaming) throws IOException {
        c(7, new Object[] { Boolean.valueOf(isStreaming) });
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return (InputStream)c(10, new Object[0]);
       /* try
        {
            Object localObject2 = localObject1;

            if (localObject2 != null)
                if ((this.m != null) && (this.m.a((InputStream)localObject2)))
                {
                    localObject2 = this.m;
                }
                else
                {
                    this.m = new MPInputStream(this, (InputStream)localObject2, this.i);
                    localObject2 = this.m;
                }
            localObject1 = localObject2;
        }
        catch (ThreadDeath localThreadDeath)
        {
            throw localThreadDeath;
        }
        catch (Throwable localThrowable)
        {

        }
        return localObject1;*/
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return (OutputStream)c(12, new Object[0]);
       /* try
        {
            Object localObject2 = localObject1;
            localac = this;
            if (localObject2 != null)
                if ((localac.l != null) && (localac.l.a((OutputStream)localObject2)))
                {
                    localObject2 = localac.l;
                }
                else
                {
                    localac.l = new MPOutputStream(localac, (OutputStream)localObject2);
                    localObject2 = localac.l;
                }
            localObject1 = localObject2;
        }
        catch (ThreadDeath localThreadDeath)
        {
            localac = null;
            throw localThreadDeath;
        }
        catch (Throwable localThrowable)
        {
            ac localac = null;
            dm.a(localThrowable);
        }
        return localObject1;*/
    }

    @Override
    protected void listen(int backlog) throws IOException {
        c(14, new Object[] { Integer.valueOf(backlog) });
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        try
        {
            c(5, new Object[] { address, Integer.valueOf(timeout) });
            return;
        }
        catch (IOException localIOException)
        {
            try
            {
                Object localObject = address;
                IOException localIOException1 = localIOException;
               // address = this;
                if ((localObject != null) && ((localObject instanceof InetSocketAddress)))
                {
                   /* MeasuredRequest localb = address.a(false);
                    localObject = (InetSocketAddress)localObject;
                    localb.e();
                    localb.a(((InetSocketAddress)localObject).getAddress());
                    localb.a(((InetSocketAddress)localObject).getPort());
                    localb.g = cf.a(localIOException1);
                    address.i.a(localb, MeasuredRequest.a.i);*/
                }
            }
            catch (ThreadDeath localThreadDeath)
            {
                throw localThreadDeath;
            }
            catch (Throwable localThrowable)
            {
                //dm.a(localThrowable);
            }
            throw localIOException;
        }
    }

    @Override
    protected void sendUrgentData(int value) throws IOException {
        c(15, new Object[] { Integer.valueOf(value) });
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return this.localSocket.getOption(optID);
    }

    @Override
    public void setOption(int optID, Object val) throws SocketException {
        this.localSocket.setOption(optID, val);
    }

    private static boolean f = false;
}