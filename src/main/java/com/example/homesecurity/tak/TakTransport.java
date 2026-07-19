package com.example.homesecurity.tak;

import java.io.IOException;

public interface TakTransport {
    void send(String cotXml) throws IOException;
}
