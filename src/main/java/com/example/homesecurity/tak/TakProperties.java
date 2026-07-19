package com.example.homesecurity.tak;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tak")
public class TakProperties {
    private boolean enabled;
    private String host = "localhost";
    private int port = 8087;
    private boolean tls;
    private String keyStore;
    private String keyStorePassword;
    private String trustStore;
    private String trustStorePassword;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration staleAfter = Duration.ofMinutes(5);
    private int batchSize = 25;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isTls() { return tls; }
    public void setTls(boolean tls) { this.tls = tls; }
    public String getKeyStore() { return keyStore; }
    public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
    public String getTrustStore() { return trustStore; }
    public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
    public String getTrustStorePassword() { return trustStorePassword; }
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getStaleAfter() { return staleAfter; }
    public void setStaleAfter(Duration staleAfter) { this.staleAfter = staleAfter; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
