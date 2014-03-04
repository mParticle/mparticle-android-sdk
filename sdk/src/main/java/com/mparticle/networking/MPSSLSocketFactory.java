package com.mparticle.networking;

import android.util.Log;

import com.mparticle.MParticle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class MPSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory localSocketFactory;

    public MPSSLSocketFactory(SSLSocketFactory socketFactory) {
        this.localSocketFactory = socketFactory;
    }

    public final SSLSocketFactory a() {
        return this.localSocketFactory;
    }

    public final String[] getDefaultCipherSuites() {
        return this.localSocketFactory.getDefaultCipherSuites();
    }

    public final String[] getSupportedCipherSuites() {
        return this.localSocketFactory.getSupportedCipherSuites();
    }

    private Socket createSocket(String host, Socket socket) {
        try {
            if (socket != null && socket instanceof SSLSocket){
                return new MPSSLSocket(host, (SSLSocket) socket);
            }
        } catch (ThreadDeath localThreadDeath) {
            throw localThreadDeath;
        } catch (Throwable localThrowable) {
            //  dm.a(localThrowable);
        }
        return socket;
    }

    public final Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Log.d(MParticle.NETWORK_TAG, "Creating socket for host: " + host);
        return createSocket(host, this.localSocketFactory.createSocket(s,host, port, autoClose));
    }

    public final Socket createSocket(String host, int port) throws IOException {
        return createSocket(host, this.localSocketFactory.createSocket(host, port));
    }

    public final Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, this.localSocketFactory.createSocket(host, port, localHost, localPort));
    }

    public final Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(address.getHostAddress(), this.localSocketFactory.createSocket(address, port));
    }

    public final Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address.getHostAddress(), this.localSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    public final Socket createSocket() throws IOException {
        return createSocket("unknown",this.localSocketFactory.createSocket());
    }
}