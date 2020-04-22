package org.nanohttpd.protocols.http.sockets;

import org.nanohttpd.util.FactoryThrowing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public interface ServerSocketFactory extends FactoryThrowing<ServerSocket, IOException> {
    InetAddress getBindAddress();

    int getBindPort();

    int getSoTimeout();
}
