package com.mparticle.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class MPSSLSocketFactory extends SSLSocketFactory
{
  private SSLSocketFactory a;


  public MPSSLSocketFactory(SSLSocketFactory paramSSLSocketFactory)
  {
    this.a = paramSSLSocketFactory;

  }

  public final SSLSocketFactory a()
  {
    return this.a;
  }

  public final String[] getDefaultCipherSuites()
  {
    return this.a.getDefaultCipherSuites();
  }

  public final String[] getSupportedCipherSuites()
  {
    return this.a.getSupportedCipherSuites();
  }

  private Socket a(Socket paramSocket)
  {
    try
    {
      if ((paramSocket != null) && ((paramSocket instanceof SSLSocket)))
        return new MPSSLSocket((SSLSocket)paramSocket);
    }
    catch (ThreadDeath localThreadDeath)
    {
      throw localThreadDeath;
    }
    catch (Throwable localThrowable)
    {
    //  dm.a(localThrowable);
    }
    return paramSocket;
  }

  public final Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    s = this.a.createSocket(s, host, port, autoClose);
    return a(s);
  }

  public final Socket createSocket(String host, int port) throws IOException {
    return a(this.a.createSocket(host, port));
  }

  public final Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {

    return a(this.a.createSocket(host, port, localHost, localPort));
  }

  public final Socket createSocket(InetAddress host, int port) throws IOException {
    return a(this.a.createSocket(host, port));
  }

  public final Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {

    return a(this.a.createSocket(address, port, localAddress, localPort));
  }

  public final Socket createSocket() throws IOException {
    Socket localSocket = this.a.createSocket();
    return a(localSocket);
  }
}