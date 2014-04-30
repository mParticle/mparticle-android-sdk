package com.mparticle;

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

/**
 * Created by sdozor on 3/4/14.
 */
final class MPSSLSocket extends SSLSocket {
    private final Queue requestQueue = new LinkedList();
    private SSLSocket localSocket;
    private OutputStream outputStream;
    private MPInputStream inputStream;

    public MPSSLSocket(String host, SSLSocket localSocket) {
        if (localSocket == null)
            throw new NullPointerException("SSLSocket was null");

        this.localSocket = localSocket;

    }

    @Override
    public final void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        this.localSocket.addHandshakeCompletedListener(listener);
    }

    @Override
    public final boolean getEnableSessionCreation() {
        return this.localSocket.getEnableSessionCreation();
    }

    @Override
    public final void setEnableSessionCreation(boolean flag) {
        this.localSocket.setEnableSessionCreation(flag);
    }

    @Override
    public final String[] getEnabledCipherSuites() {
        return this.localSocket.getEnabledCipherSuites();
    }

    @Override
    public final void setEnabledCipherSuites(String[] suites) {
        this.localSocket.setEnabledCipherSuites(suites);
    }

    @Override
    public final String[] getEnabledProtocols() {
        return this.localSocket.getEnabledProtocols();
    }

    @Override
    public final void setEnabledProtocols(String[] protocols) {
        this.localSocket.setEnabledProtocols(protocols);
    }

    @Override
    public final boolean getNeedClientAuth() {
        return this.localSocket.getNeedClientAuth();
    }

    @Override
    public final void setNeedClientAuth(boolean need) {
        this.localSocket.setNeedClientAuth(need);
    }

    @Override
    public final SSLSession getSession() {
        return this.localSocket.getSession();
    }

    @Override
    public final String[] getSupportedCipherSuites() {
        return this.localSocket.getSupportedCipherSuites();
    }

    @Override
    public final String[] getSupportedProtocols() {
        return this.localSocket.getSupportedProtocols();
    }

    @Override
    public final boolean getUseClientMode() {
        return this.localSocket.getUseClientMode();
    }

    @Override
    public final void setUseClientMode(boolean mode) {
        this.localSocket.setUseClientMode(mode);
    }

    @Override
    public final boolean getWantClientAuth() {
        return this.localSocket.getWantClientAuth();
    }

    @Override
    public final void setWantClientAuth(boolean want) {
        this.localSocket.setWantClientAuth(want);
    }

    @Override
    public final void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        this.localSocket.removeHandshakeCompletedListener(listener);
    }

    @Override
    public final void startHandshake() throws IOException {
        this.localSocket.startHandshake();
    }

    @Override
    public final void bind(SocketAddress localAddr) throws IOException {
        this.localSocket.bind(localAddr);
    }

    @Override
    public final void close() throws IOException {
        this.localSocket.close();
    }

    @Override
    public final void connect(SocketAddress remoteAddr, int timeout) throws IOException {
        this.localSocket.connect(remoteAddr, timeout);
    }

    @Override
    public final void connect(SocketAddress remoteAddr) throws IOException {
        this.localSocket.connect(remoteAddr);
    }

    @Override
    public final SocketChannel getChannel() {
        return this.localSocket.getChannel();
    }

    @Override
    public final InetAddress getInetAddress() {
        return this.localSocket.getInetAddress();
    }

    @Override
    public final InputStream getInputStream() throws IOException {
       InputStream inputStreams = this.localSocket.getInputStream();
       if (inputStreams != null){
            if (this.inputStream != null && inputStream.isSameStream(inputStreams)){
                return this.inputStream;
            }else{
                this.inputStream = new MPInputStream(inputStreams);
            }
        }
        return this.inputStream;
    }

    @Override
    public final boolean getKeepAlive() throws SocketException {
        return this.localSocket.getKeepAlive();
    }

    @Override
    public final void setKeepAlive(boolean keepAlive) throws SocketException {
        this.localSocket.setKeepAlive(keepAlive);
    }

    @Override
    public final InetAddress getLocalAddress() {
        return this.localSocket.getLocalAddress();
    }

    @Override
    public final int getLocalPort() {
        return this.localSocket.getLocalPort();
    }

    @Override
    public final SocketAddress getLocalSocketAddress() {
        return this.localSocket.getLocalSocketAddress();
    }

    @Override
    public final boolean getOOBInline() throws SocketException {
        return this.localSocket.getOOBInline();
    }

    @Override
    public final void setOOBInline(boolean oobinline) throws SocketException {
        this.localSocket.setOOBInline(oobinline);
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        OutputStream outputStream =this.localSocket.getOutputStream();
        if (outputStream != null){
            if (this.outputStream != null){
                return this.outputStream;
            }else{
                this.outputStream = new MPOutputStream(outputStream, inputStream);
            }
        }
        return this.outputStream;
    }

    @Override
    public final int getPort() {
        return this.localSocket.getPort();
    }

    @Override
    public final int getReceiveBufferSize() throws SocketException {
        return this.localSocket.getReceiveBufferSize();
    }

    @Override
    public final void setReceiveBufferSize(int size) throws SocketException {
        this.localSocket.setReceiveBufferSize(size);
    }

    @Override
    public final SocketAddress getRemoteSocketAddress() {
        return this.localSocket.getRemoteSocketAddress();
    }

    @Override
    public final boolean getReuseAddress() throws SocketException {
        return this.localSocket.getReuseAddress();
    }

    @Override
    public final void setReuseAddress(boolean reuse) throws SocketException {
        this.localSocket.setReuseAddress(reuse);
    }

    @Override
    public final int getSendBufferSize() throws SocketException {
        return this.localSocket.getSendBufferSize();
    }

    @Override
    public final void setSendBufferSize(int size) throws SocketException {
        this.localSocket.setSendBufferSize(size);
    }

    @Override
    public final int getSoLinger() throws SocketException {
        return this.localSocket.getSoLinger();
    }

    @Override
    public final int getSoTimeout() throws SocketException {
        return this.localSocket.getSoTimeout();
    }

    @Override
    public final void setSoTimeout(int timeout) throws SocketException {
        this.localSocket.setSoTimeout(timeout);
    }

    @Override
    public final boolean getTcpNoDelay() throws SocketException {
        return this.localSocket.getTcpNoDelay();
    }

    @Override
    public final void setTcpNoDelay(boolean on) throws SocketException {
        this.localSocket.setTcpNoDelay(on);
    }

    @Override
    public final int getTrafficClass() throws SocketException {
        return this.localSocket.getTrafficClass();
    }

    @Override
    public final void setTrafficClass(int value) throws SocketException {
        this.localSocket.setTrafficClass(value);
    }

    @Override
    public final boolean isBound() {
        return this.localSocket.isBound();
    }

    @Override
    public final boolean isClosed() {
        return this.localSocket.isClosed();
    }

    @Override
    public final boolean isConnected() {
        return this.localSocket.isConnected();
    }

    @Override
    public final boolean isInputShutdown() {
        return this.localSocket.isInputShutdown();
    }

    @Override
    public final boolean isOutputShutdown() {
        return this.localSocket.isOutputShutdown();
    }

    @Override
    public final void sendUrgentData(int value) throws IOException {
        this.localSocket.sendUrgentData(value);
    }

    @Override
    public final void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        this.localSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public final void setSoLinger(boolean on, int timeout) throws SocketException {
        this.localSocket.setSoLinger(on, timeout);
    }

    @Override
    public final void shutdownInput() throws IOException {
        this.localSocket.shutdownInput();
    }

    @Override
    public final void shutdownOutput() throws IOException {
        this.localSocket.shutdownOutput();
    }

    @Override
    public final String toString() {
        return this.localSocket.toString();
    }

    @Override
    public final boolean equals(Object o) {
        return this.localSocket.equals(o);
    }

    @Override
    public final int hashCode() {
        return this.localSocket.hashCode();
    }
}