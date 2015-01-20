package com.mparticle.internal.np;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

final class MPSocketImpl extends SocketImpl {
    private static Field addressField;
    private static Field fileDescriptorField;
    private static Field localPortField;
    private static Field portField;
    private static Method[] methods = new Method[20];
    private SocketImpl delegateSocket;
    private MPInputStream delegateInputStream;
    private MPOutputStream delegateOutputStream;

    public MPSocketImpl(SocketImpl socketImpl) throws Exception{
        delegateSocket = socketImpl;

        updateFields();
    }

    static {
        try{

            Class localSocketImpl = SocketImpl.class;
            addressField = localSocketImpl.getDeclaredField("address");
            fileDescriptorField = localSocketImpl.getDeclaredField("fd");
            localPortField = localSocketImpl.getDeclaredField("localport");
            portField = localSocketImpl.getDeclaredField("port");
            AccessibleObject[] objects = {addressField, fileDescriptorField, localPortField, portField};
            setAccessible(objects);
            methods[0] = localSocketImpl.getDeclaredMethod("accept", new Class[]{SocketImpl.class});
            methods[1] = localSocketImpl.getDeclaredMethod("available", new Class[0]);
            methods[2] = localSocketImpl.getDeclaredMethod("bind", new Class[]{InetAddress.class, Integer.TYPE});
            methods[3] = localSocketImpl.getDeclaredMethod("close", new Class[0]);
            methods[4] = localSocketImpl.getDeclaredMethod("connect", new Class[]{InetAddress.class, Integer.TYPE});
            methods[5] = localSocketImpl.getDeclaredMethod("connect", new Class[]{SocketAddress.class, Integer.TYPE});
            methods[6] = localSocketImpl.getDeclaredMethod("connect", new Class[]{String.class, Integer.TYPE});
            methods[7] = localSocketImpl.getDeclaredMethod("create", new Class[]{Boolean.TYPE});
            methods[8] = localSocketImpl.getDeclaredMethod("getFileDescriptor", new Class[0]);
            methods[9] = localSocketImpl.getDeclaredMethod("getInetAddress", new Class[0]);
            methods[10] = localSocketImpl.getDeclaredMethod("getInputStream", new Class[0]);
            methods[11] = localSocketImpl.getDeclaredMethod("getLocalPort", new Class[0]);
            methods[12] = localSocketImpl.getDeclaredMethod("getOutputStream", new Class[0]);
            methods[13] = localSocketImpl.getDeclaredMethod("getPort", new Class[0]);
            methods[14] = localSocketImpl.getDeclaredMethod("listen", new Class[]{Integer.TYPE});
            methods[15] = localSocketImpl.getDeclaredMethod("sendUrgentData", new Class[]{Integer.TYPE});
            methods[16] = localSocketImpl.getDeclaredMethod("setPerformancePreferences", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE});
            methods[17] = localSocketImpl.getDeclaredMethod("shutdownInput", new Class[0]);
            methods[18] = localSocketImpl.getDeclaredMethod("shutdownOutput", new Class[0]);
            methods[19] = localSocketImpl.getDeclaredMethod("supportsUrgentData", new Class[0]);
            setAccessible(methods);
        }catch (Exception e){

        }
    }

    public static void setAccessible(AccessibleObject[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                objects[i].setAccessible(true);
            }
        }
    }

    private Object invokeMethod(int paramInt, Object[] paramArrayOfObject) throws IOException {
        try {
            addressField.set(delegateSocket, address);
            fileDescriptorField.set(delegateSocket, fd);
            localPortField.setInt(delegateSocket, localport);
            portField.setInt(delegateSocket, port);
        } catch (Exception e) {
        }

        try {
            return methods[paramInt].invoke(delegateSocket, paramArrayOfObject);
        } catch (Exception e) {
            //e.printStackTrace();

            //important: this methods are *supposed* to throw sometimes
            if (e instanceof  InvocationTargetException && ((InvocationTargetException) e).getTargetException() instanceof IOException){
                throw (IOException)((InvocationTargetException) e).getTargetException();
            }else if (e instanceof IOException){
                throw (IOException)e;
            }
        }finally {
            updateFields();
        }

        return null;
    }

    private void updateFields() throws IOException {
        try {
            this.address = ((InetAddress) addressField.get(delegateSocket));
            this.fd = ((FileDescriptor) fileDescriptorField.get(delegateSocket));
            this.localport = localPortField.getInt(delegateSocket);
            this.port = portField.getInt(delegateSocket);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        invokeMethod(0, new Object[]{s});
    }

    @Override
    protected int available() throws IOException {
        Integer localInteger = (Integer) invokeMethod(1, new Object[0]);
        return localInteger.intValue();
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        invokeMethod(2, new Object[]{host, Integer.valueOf(port)});
    }

    @Override
    protected void close() throws IOException {
        invokeMethod(3, new Object[0]);
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        invokeMethod(6, new Object[]{host, Integer.valueOf(port)});
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        invokeMethod(4, new Object[]{address, Integer.valueOf(port)});
    }

    @Override
    protected void create(boolean isStreaming) throws IOException {
        invokeMethod(7, new Object[]{Boolean.valueOf(isStreaming)});
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        InputStream inputStream = (InputStream) invokeMethod(10, new Object[0]);
        if (inputStream != null) {
            if (delegateInputStream == null || !delegateInputStream.isSameStream(inputStream)) {
                delegateInputStream = new MPInputStream(inputStream);
            }
        } else {
            return inputStream;
        }
        return delegateInputStream;
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        OutputStream outputStream =(OutputStream) invokeMethod(12, new Object[0]);
        if (outputStream != null) {
            if (delegateOutputStream == null || !delegateOutputStream.isSameStream(outputStream)) {
                delegateOutputStream = new MPOutputStream(outputStream, delegateInputStream);
            }
        } else {
            return outputStream;
        }
        return delegateOutputStream;
    }

    @Override
    protected void listen(int backlog) throws IOException {
        invokeMethod(14, new Object[]{Integer.valueOf(backlog)});
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        invokeMethod(5, new Object[]{address, Integer.valueOf(timeout)});
    }

    @Override
    protected void sendUrgentData(int value) throws IOException {
        invokeMethod(15, new Object[]{Integer.valueOf(value)});
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return delegateSocket.getOption(optID);
    }

    @Override
    public void setOption(int optID, Object val) throws SocketException {
        delegateSocket.setOption(optID, val);
    }
}