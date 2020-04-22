package org.nanohttpd.protocols.http.sockets;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketImpl;

abstract class ServerSocketFactoryImpl implements ServerSocketFactory {
    private final InetAddress mBindAddress;
    private final int mBindPort;
    private final int mSoTimeout;

    public ServerSocketFactoryImpl(InetAddress bindAddress, int bindPort, int timeout) {
        mBindAddress = bindAddress;
        mBindPort = bindPort;
        mSoTimeout = timeout;

        if (bindPort < 0)
            throw new IllegalArgumentException("The port cannot be below 0.");
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
