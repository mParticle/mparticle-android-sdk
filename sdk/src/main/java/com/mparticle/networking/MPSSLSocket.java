package com.mparticle.networking;

import android.util.Log;

import com.mparticle.MParticle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public final class MPSSLSocket extends SSLSocket implements ISocket {
    private final Queue requestQueue = new LinkedList();
    private SSLSocket localSocket;
    private OutputStream outputStream;
    private MPInputStream inputStream;
    private MeasuredRequest request;

    public MPSSLSocket(String host, SSLSocket localSocket) {
        if (localSocket == null)
            throw new NullPointerException("SSLSocket was null");

        this.localSocket = localSocket;
        request = new MeasuredRequest(host);

    }

    public final void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        this.localSocket.addHandshakeCompletedListener(listener);
    }

    public final boolean getEnableSessionCreation() {
        return this.localSocket.getEnableSessionCreation();
    }

    public final void setEnableSessionCreation(boolean flag) {
        this.localSocket.setEnableSessionCreation(flag);
    }

    public final String[] getEnabledCipherSuites() {
        return this.localSocket.getEnabledCipherSuites();
    }

    public final void setEnabledCipherSuites(String[] suites) {
        this.localSocket.setEnabledCipherSuites(suites);
    }

    public final String[] getEnabledProtocols() {
        return this.localSocket.getEnabledProtocols();
    }

    public final void setEnabledProtocols(String[] protocols) {
        this.localSocket.setEnabledProtocols(protocols);
    }

    public final boolean getNeedClientAuth() {
        return this.localSocket.getNeedClientAuth();
    }

    public final void setNeedClientAuth(boolean need) {
        this.localSocket.setNeedClientAuth(need);
    }

    public final SSLSession getSession() {
        return this.localSocket.getSession();
    }

    public final String[] getSupportedCipherSuites() {
        return this.localSocket.getSupportedCipherSuites();
    }

    public final String[] getSupportedProtocols() {
        return this.localSocket.getSupportedProtocols();
    }

    public final boolean getUseClientMode() {
        return this.localSocket.getUseClientMode();
    }

    public final void setUseClientMode(boolean mode) {
        this.localSocket.setUseClientMode(mode);
    }

    public final boolean getWantClientAuth() {
        return this.localSocket.getWantClientAuth();
    }

    public final void setWantClientAuth(boolean want) {
        this.localSocket.setWantClientAuth(want);
    }

    public final void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        this.localSocket.removeHandshakeCompletedListener(listener);
    }

    public final void startHandshake() throws IOException {
        this.localSocket.startHandshake();
    }

    public final void bind(SocketAddress localAddr) throws IOException {
        this.localSocket.bind(localAddr);
    }

    public final void close() throws IOException {
        this.localSocket.close();
    }

    public final void connect(SocketAddress remoteAddr, int timeout) throws IOException {
        this.localSocket.connect(remoteAddr, timeout);
    }

    public final void connect(SocketAddress remoteAddr) throws IOException {
        this.localSocket.connect(remoteAddr);
    }

    public final SocketChannel getChannel() {
        return this.localSocket.getChannel();
    }

    public final InetAddress getInetAddress() {
        return this.localSocket.getInetAddress();
    }

    public final InputStream getInputStream() throws IOException {
        InputStream inputStreams = this.localSocket.getInputStream();
        if (inputStreams != null){
            if (this.inputStream != null && inputStream.isSameStream(inputStreams)){
                return this.inputStream;
            }else{
                this.inputStream = new MPInputStream(inputStreams, request);
            }
        }
        return this.inputStream;
    }

    public final boolean getKeepAlive() throws SocketException {
        return this.localSocket.getKeepAlive();
    }

    public final void setKeepAlive(boolean keepAlive) throws SocketException {
        this.localSocket.setKeepAlive(keepAlive);
    }

    public final InetAddress getLocalAddress() {
        return this.localSocket.getLocalAddress();
    }

    public final int getLocalPort() {
        return this.localSocket.getLocalPort();
    }

    public final SocketAddress getLocalSocketAddress() {
        return this.localSocket.getLocalSocketAddress();
    }

    public final boolean getOOBInline() throws SocketException {
        return this.localSocket.getOOBInline();
    }

    public final void setOOBInline(boolean oobinline) throws SocketException {
        this.localSocket.setOOBInline(oobinline);
    }

    public final OutputStream getOutputStream() throws IOException {
        this.request.startTiming();
        OutputStream outputStream = this.localSocket.getOutputStream();
        if (outputStream != null){
            if (this.outputStream != null){
                return this.outputStream;
            }else{
                this.outputStream = new MPOutputStream(outputStream, request);
            }
        }
        return this.outputStream;
    }

    public final int getPort() {
        return this.localSocket.getPort();
    }

    public final int getReceiveBufferSize() throws SocketException {
        return this.localSocket.getReceiveBufferSize();
    }

    public final void setReceiveBufferSize(int size) throws SocketException {
        this.localSocket.setReceiveBufferSize(size);
    }

    public final SocketAddress getRemoteSocketAddress() {
        return this.localSocket.getRemoteSocketAddress();
    }

    public final boolean getReuseAddress() throws SocketException {
        return this.localSocket.getReuseAddress();
    }

    public final void setReuseAddress(boolean reuse) throws SocketException {
        this.localSocket.setReuseAddress(reuse);
    }

    public final int getSendBufferSize() throws SocketException {
        return this.localSocket.getSendBufferSize();
    }

    public final void setSendBufferSize(int size) throws SocketException {
        this.localSocket.setSendBufferSize(size);
    }

    public final int getSoLinger() throws SocketException {
        return this.localSocket.getSoLinger();
    }

    public final int getSoTimeout() throws SocketException {
        return this.localSocket.getSoTimeout();
    }

    public final void setSoTimeout(int timeout) throws SocketException {
        this.localSocket.setSoTimeout(timeout);
    }

    public final boolean getTcpNoDelay() throws SocketException {
        return this.localSocket.getTcpNoDelay();
    }

    public final void setTcpNoDelay(boolean on) throws SocketException {
        this.localSocket.setTcpNoDelay(on);
    }

    public final int getTrafficClass() throws SocketException {
        return this.localSocket.getTrafficClass();
    }

    public final void setTrafficClass(int value) throws SocketException {
        this.localSocket.setTrafficClass(value);
    }

    public final boolean isBound() {
        return this.localSocket.isBound();
    }

    public final boolean isClosed() {
        return this.localSocket.isClosed();
    }

    public final boolean isConnected() {
        return this.localSocket.isConnected();
    }

    public final boolean isInputShutdown() {
        return this.localSocket.isInputShutdown();
    }

    public final boolean isOutputShutdown() {
        return this.localSocket.isOutputShutdown();
    }

    public final void sendUrgentData(int value) throws IOException {
        this.localSocket.sendUrgentData(value);
    }

    public final void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        this.localSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    public final void setSoLinger(boolean on, int timeout) throws SocketException {
        this.localSocket.setSoLinger(on, timeout);
    }

    public final void shutdownInput() throws IOException {
        this.localSocket.shutdownInput();
    }

    public final void shutdownOutput() throws IOException {
        this.localSocket.shutdownOutput();
    }

    public final String toString() {
        return this.localSocket.toString();
    }

    public final boolean equals(Object o) {
        return this.localSocket.equals(o);
    }

    public final int hashCode() {
        return this.localSocket.hashCode();
    }

    @Override
    public MeasuredRequest a() {
        return null;
    }

    public final void a(MeasuredRequest paramb) {
        if (paramb != null)
            synchronized (this.requestQueue) {
                this.requestQueue.add(paramb);
                return;
            }
    }

    public final MeasuredRequest b() {
        synchronized (this.requestQueue) {
            return (MeasuredRequest) this.requestQueue.poll();
        }
    }
}