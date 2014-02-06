package com.mparticle;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public final class MPSSLSocket extends SSLSocket
  implements ISocket
{
  private SSLSocket a;

  private final Queue d = new LinkedList();
  private OutputStream e;
  private InputStream f;

  public MPSSLSocket(SSLSocket paramSSLSocket)
  {
    if (paramSSLSocket == null)
      throw new NullPointerException("delegate was null");

    this.a = paramSSLSocket;

  }

  public final void addHandshakeCompletedListener(HandshakeCompletedListener listener)
  {
    this.a.addHandshakeCompletedListener(listener);
  }

  public final boolean getEnableSessionCreation()
  {
    return this.a.getEnableSessionCreation();
  }

  public final String[] getEnabledCipherSuites()
  {
    return this.a.getEnabledCipherSuites();
  }

  public final String[] getEnabledProtocols()
  {
    return this.a.getEnabledProtocols();
  }

  public final boolean getNeedClientAuth()
  {
    return this.a.getNeedClientAuth();
  }

  public final SSLSession getSession()
  {
    return this.a.getSession();
  }

  public final String[] getSupportedCipherSuites()
  {
    return this.a.getSupportedCipherSuites();
  }

  public final String[] getSupportedProtocols()
  {
    return this.a.getSupportedProtocols();
  }

  public final boolean getUseClientMode()
  {
    return this.a.getUseClientMode();
  }

  public final boolean getWantClientAuth()
  {
    return this.a.getWantClientAuth();
  }

  public final void removeHandshakeCompletedListener(HandshakeCompletedListener listener)
  {
    this.a.removeHandshakeCompletedListener(listener);
  }

  public final void setEnableSessionCreation(boolean flag)
  {
    this.a.setEnableSessionCreation(flag);
  }

  public final void setEnabledCipherSuites(String[] suites)
  {
    this.a.setEnabledCipherSuites(suites);
  }

  public final void setEnabledProtocols(String[] protocols)
  {
    this.a.setEnabledProtocols(protocols);
  }

  public final void setNeedClientAuth(boolean need)
  {
    this.a.setNeedClientAuth(need);
  }

  public final void setUseClientMode(boolean mode)
  {
    this.a.setUseClientMode(mode);
  }

  public final void setWantClientAuth(boolean want)
  {
    this.a.setWantClientAuth(want);
  }

  public final void startHandshake() throws IOException
  {
    try
    {
      this.a.startHandshake();
      return;
    }
    catch (IOException localIOException1)
    {
      IOException localIOException2 = localIOException1;
      MPSSLSocket localaa = this;
      try
      {

      }
      catch (ThreadDeath localThreadDeath)
      {
        throw localThreadDeath;
      }
      catch (Throwable localThrowable)
      {
       // dm.a(localThrowable);
      }
      throw localIOException1;
    }
  }

  public final void bind(SocketAddress localAddr) throws IOException
  {
      Log.d(Constants.LOG_TAG, "Bind " + localAddr.toString());
    this.a.bind(localAddr);
  }

  public final void close() throws IOException
  {
    this.a.close();
    try
    {
     // if (this.f != null)
       // this.f.d();
      return;
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
    //  dm.a(localThrowable);
    }
  }

  public final void connect(SocketAddress remoteAddr, int timeout) throws IOException
  {
      Log.d(Constants.LOG_TAG, "Connect " + remoteAddr.toString());
    this.a.connect(remoteAddr, timeout);
  }

  public final void connect(SocketAddress remoteAddr) throws IOException
  {
      Log.d(Constants.LOG_TAG, "Connect " + remoteAddr.toString());
    this.a.connect(remoteAddr);
  }

  public final SocketChannel getChannel()
  {
    return this.a.getChannel();
  }

  public final InetAddress getInetAddress()
  {
    return this.a.getInetAddress();
  }

  public final InputStream getInputStream() throws IOException
  {
    Object localObject1 = this.a.getInputStream();
      MPSSLSocket localaa = this;
    try
    {
      Object localObject2 = localObject1;

      if (localObject2 != null)
        if ((localaa.f != null))// && (localaa.f.a((InputStream)localObject2)))
        {
          localObject2 = localaa.f;
        }
        else
        {
          localaa.f = new MPInputStream(localaa, (InputStream)localObject2);
          localObject2 = localaa.f;
        }
      localObject1 = localObject2;
    }
    catch (ThreadDeath localThreadDeath)
    {
      localaa = null;
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {

    //  dm.a(localThrowable);
    }
    return (InputStream) localObject1;
  }

  public final boolean getKeepAlive() throws SocketException
  {
    return this.a.getKeepAlive();
  }

  public final InetAddress getLocalAddress()
  {
    return this.a.getLocalAddress();
  }

  public final int getLocalPort()
  {
    return this.a.getLocalPort();
  }

  public final SocketAddress getLocalSocketAddress()
  {
    return this.a.getLocalSocketAddress();
  }

  public final boolean getOOBInline() throws SocketException {
    return this.a.getOOBInline();
  }

  public final OutputStream getOutputStream() throws IOException {
    Object localObject1 = this.a.getOutputStream();
      MPSSLSocket localaa = this;
    try
    {
      Object localObject2 = localObject1;

      if (localObject2 != null)
        if ((localaa.e != null))// && (localaa.e.a((OutputStream)localObject2)))
        {
          localObject2 = localaa.e;
        }
        else
        {
          localaa.e = new MPOutputStream(localaa, (OutputStream)localObject2);
          localObject2 = localaa.e;
        }
      localObject1 = localObject2;
    }
    catch (ThreadDeath localThreadDeath)
    {
      localaa = null;
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {

    }
    return (OutputStream)localObject1;
  }

  public final int getPort()
  {
    return this.a.getPort();
  }

  public final int getReceiveBufferSize() throws SocketException {
    return this.a.getReceiveBufferSize();
  }

  public final SocketAddress getRemoteSocketAddress()
  {
    return this.a.getRemoteSocketAddress();
  }

  public final boolean getReuseAddress() throws SocketException {
    return this.a.getReuseAddress();
  }

  public final int getSendBufferSize() throws SocketException {
    return this.a.getSendBufferSize();
  }

  public final int getSoLinger() throws SocketException {
    return this.a.getSoLinger();
  }

  public final int getSoTimeout() throws SocketException {
    return this.a.getSoTimeout();
  }

  public final boolean getTcpNoDelay() throws SocketException {
    return this.a.getTcpNoDelay();
  }

  public final int getTrafficClass() throws SocketException {
    return this.a.getTrafficClass();
  }

  public final boolean isBound()
  {
    return this.a.isBound();
  }

  public final boolean isClosed()
  {
    return this.a.isClosed();
  }

  public final boolean isConnected()
  {
    return this.a.isConnected();
  }

  public final boolean isInputShutdown()
  {
    return this.a.isInputShutdown();
  }

  public final boolean isOutputShutdown()
  {
    return this.a.isOutputShutdown();
  }

  public final void sendUrgentData(int value) throws IOException {
    this.a.sendUrgentData(value);
  }

  public final void setKeepAlive(boolean keepAlive) throws SocketException {
    this.a.setKeepAlive(keepAlive);
  }

  public final void setOOBInline(boolean oobinline) throws SocketException {
    this.a.setOOBInline(oobinline);
  }

  public final void setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    this.a.setPerformancePreferences(connectionTime, latency, bandwidth);
  }

  public final void setReceiveBufferSize(int size) throws SocketException {
    this.a.setReceiveBufferSize(size);
  }

  public final void setReuseAddress(boolean reuse) throws SocketException {
    this.a.setReuseAddress(reuse);
  }

  public final void setSendBufferSize(int size) throws SocketException {
    this.a.setSendBufferSize(size);
  }

  public final void setSoLinger(boolean on, int timeout) throws SocketException {
    this.a.setSoLinger(on, timeout);
  }

  public final void setSoTimeout(int timeout) throws SocketException {
    this.a.setSoTimeout(timeout);
  }

  public final void setTcpNoDelay(boolean on) throws SocketException {
    this.a.setTcpNoDelay(on);
  }

  public final void setTrafficClass(int value) throws SocketException {
    this.a.setTrafficClass(value);
  }

  public final void shutdownInput() throws IOException {
    this.a.shutdownInput();
  }

  public final void shutdownOutput() throws IOException {
    this.a.shutdownOutput();
  }

  public final String toString()
  {
    return this.a.toString();
  }

  public final boolean equals(Object o)
  {
    return this.a.equals(o);
  }

  public final int hashCode()
  {
    return this.a.hashCode();
  }

  public final MeasuredRequest a()
  {
    return a(false);
  }

  private MeasuredRequest a(boolean paramBoolean)
  {
    MeasuredRequest localb = new MeasuredRequest();
//
    return localb;
  }

  public final void a(MeasuredRequest paramb)
  {
    if (paramb != null)
      synchronized (this.d)
      {
        this.d.add(paramb);
        return;
      }
  }

  public final MeasuredRequest b()
  {
    synchronized (this.d)
    {
      return (MeasuredRequest)this.d.poll();
    }
  }
}