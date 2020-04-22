package org.nanohttpd.protocols.http.sockets;

import org.nanohttpd.protocols.http.NanoHTTPD;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class SecureSockets {
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an
     * array of loaded KeyManagers. These objects must properly
     * loaded/initialized by the caller.
     */
    public static SSLServerSocketFactory createServerSocketFactory(KeyStore loadedKeyStore, KeyManager[] keyManagers)
            throws IOException {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
            return ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and a
     * loaded KeyManagerFactory. These objects must properly loaded/initialized
     * by the caller.
     */
    public static SSLServerSocketFactory createServerSocketFactory(KeyStore keyStore, KeyManagerFactory managerFactory)
            throws IOException {
        try {
            return createServerSocketFactory(keyStore, managerFactory.getKeyManagers());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a KeyStore resource with your
     * certificate and passphrase
     */
    public static SSLServerSocketFactory createServerSocketFactory(String storeClasspath, char[] passphrase)
            throws IOException {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = NanoHTTPD.class.getResourceAsStream(storeClasspath);

            if (keystoreStream == null)
                throw new IOException("Unable to load keystore from classpath: " + storeClasspath);

            keystore.load(keystoreStream, passphrase);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            return createServerSocketFactory(keystore, keyManagerFactory);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
