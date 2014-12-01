package com.mparticle.internal.np;

import com.mparticle.MParticle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by sdozor on 3/4/14.
 */
public final class MPSSLSocketFactory extends SSLSocketFactory {
    public SSLSocketFactory delegateFactory;

    public MPSSLSocketFactory(SSLSocketFactory socketFactory) {
        this.delegateFactory = socketFactory;
    }

    public final String[] getDefaultCipherSuites() {
        return this.delegateFactory.getDefaultCipherSuites();
    }

    public final String[] getSupportedCipherSuites() {
        return this.delegateFactory.getSupportedCipherSuites();
    }

    private Socket createSocket(String host, Socket socket) throws IOException{
        if (socket != null && socket instanceof SSLSocket && MParticle.getInstance().internal().shouldProcessUrl(host)){
            return new MPSSLSocket(host, (SSLSocket) socket);
        }
        return socket;
    }

    @Override
    public final Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return createSocket(host, this.delegateFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public final Socket createSocket(String host, int port) throws IOException {
        return createSocket(host, this.delegateFactory.createSocket(host, port));
    }

    @Override
    public final Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, this.delegateFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public final Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(address.getHostAddress(), this.delegateFactory.createSocket(address, port));
    }

    @Override
    public final Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address.getHostAddress(), this.delegateFactory.createSocket(address, port, localAddress, localPort));
    }

    @Override
    public final Socket createSocket() throws IOException {
        return createSocket("unknown", this.delegateFactory.createSocket());
    }
}