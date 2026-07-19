package com.example.homesecurity.tak;

import com.example.homesecurity.event.EventType;
import com.example.homesecurity.event.SecurityEvent;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CotConverterTest {
    @Test
    void createsWellFormedCotWithMappedFieldsAndEscapedText() throws Exception {
        TakProperties properties = new TakProperties();
        properties.setStaleAfter(Duration.ofMinutes(10));
        SecurityEvent event = new SecurityEvent("evt&1", "esp32-front", EventType.ACCESS_DENIED,
                "Front <Door>", 39.7392, -104.9903, true,
                Instant.parse("2026-07-18T12:00:00Z"));

        String xml = new CotConverter(properties).convert(event);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(document.getDocumentElement().getAttribute("type")).isEqualTo("b-m-p-s-a-d");
        assertThat(document.getDocumentElement().getAttribute("stale"))
                .isEqualTo("2026-07-18T12:10:00Z");
        assertThat(document.getElementsByTagName("point").item(0).getAttributes()
                .getNamedItem("lat").getNodeValue()).isEqualTo("39.7392");
        assertThat(document.getElementsByTagName("remarks").item(0).getTextContent())
                .contains("Front <Door>");
    }
}
