package org.nanohttpd.protocols.http.sockets;

import java.net.InetAddress;

abstract class ServerSocketFactoryImpl implements ServerSocketFactory {
    private final InetAddress mBindAddress;
    private final int mBindPort;
    private final int mSoTimeout;

    public ServerSocketFactoryImpl(InetAddress bindAddress, int bindPort, int timeout) {
        mBindAddress = bindAddress;
        mBindPort = bindPort;
        mSoTimeout = timeout;
    }

    @Override
    public InetAddress getBindAddress() {
        return mBindAddress;
    }

    @Override
    public int getBindPort() {
        return mBindPort;
    }

    @Override
    public int getSoTimeout() {
        return mSoTimeout;
    }
}
