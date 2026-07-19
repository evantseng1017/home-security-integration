package com.example.homesecurity.tak;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SocketTakTransportTest {
    @Test
    void sendsCotToLocalMockTakReceiver() throws Exception {
        try (ServerSocket receiver = new ServerSocket(0)) {
            CompletableFuture<String> received = CompletableFuture.supplyAsync(() -> {
                try (var socket = receiver.accept();
                     var reader = new BufferedReader(new InputStreamReader(
                             socket.getInputStream(), StandardCharsets.UTF_8))) {
                    return reader.readLine();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            TakProperties properties = new TakProperties();
            properties.setHost("127.0.0.1");
            properties.setPort(receiver.getLocalPort());
            properties.setConnectTimeout(Duration.ofSeconds(1));
            new SocketTakTransport(properties).send("<event uid=\"test-event\"/>");

            assertThat(received.get(2, TimeUnit.SECONDS)).isEqualTo("<event uid=\"test-event\"/>");
        }
    }
}
