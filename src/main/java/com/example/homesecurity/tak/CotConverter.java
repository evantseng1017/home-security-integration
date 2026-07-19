package com.example.homesecurity.tak;

import com.example.homesecurity.event.SecurityEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CotConverter {
    private final TakProperties properties;

    public CotConverter(TakProperties properties) {
        this.properties = properties;
    }

    public String convert(SecurityEvent event) {
        Instant stale = event.getTimestamp().plus(properties.getStaleAfter());
        String uid = escape("security-" + event.getDeviceId() + "-" + event.getEventId());
        String callsign = escape(event.getLocationName() + " Security");
        String remarks = escape(event.getEventType() + " at " + event.getLocationName()
                + "; armed=" + event.isArmed());

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <event version="2.0" uid="%s" type="%s" time="%s" start="%s" stale="%s" how="m-g">
                  <point lat="%s" lon="%s" hae="0" ce="10" le="10"/>
                  <detail>
                    <contact callsign="%s"/>
                    <remarks>%s</remarks>
                    <securityEvent eventId="%s" deviceId="%s" eventType="%s" armed="%s"/>
                  </detail>
                </event>
                """.formatted(uid, CotTypeMapping.forEvent(event.getEventType()), event.getTimestamp(),
                event.getTimestamp(), stale, event.getLatitude(), event.getLongitude(), callsign, remarks,
                escape(event.getEventId()), escape(event.getDeviceId()), event.getEventType(), event.isArmed());
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
