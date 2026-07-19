package com.example.homesecurity.tak;

import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@Component
public class SocketTakTransport implements TakTransport {
    private final TakProperties properties;

    public SocketTakTransport(TakProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String cotXml) throws IOException {
        try (Socket socket = createSocket()) {
            socket.connect(new InetSocketAddress(properties.getHost(), properties.getPort()),
                    Math.toIntExact(properties.getConnectTimeout().toMillis()));
            socket.getOutputStream().write((cotXml + "\n").getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
        }
    }

    private Socket createSocket() throws IOException {
        if (!properties.isTls()) {
            return new Socket();
        }
        try {
            return sslContext().getSocketFactory().createSocket();
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to initialize TAK TLS configuration", e);
        }
    }

    private SSLContext sslContext() throws GeneralSecurityException, IOException {
        if (isBlank(properties.getKeyStore()) || isBlank(properties.getTrustStore())) {
            throw new IOException("tak.key-store and tak.trust-store are required when tak.tls=true");
        }

        KeyStore keys = loadStore(properties.getKeyStore(), properties.getKeyStorePassword());
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keys, chars(properties.getKeyStorePassword()));

        KeyStore trust = loadStore(properties.getTrustStore(), properties.getTrustStorePassword());
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(trust);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);
        return context;
    }

    private KeyStore loadStore(String location, String password) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(Path.of(location))) {
            store.load(input, chars(password));
        }
        return store;
    }

    private char[] chars(String value) { return value == null ? new char[0] : value.toCharArray(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
