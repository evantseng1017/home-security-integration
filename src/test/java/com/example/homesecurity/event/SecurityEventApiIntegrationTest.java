package com.example.homesecurity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//test
@SpringBootTest
@AutoConfigureMockMvc
class SecurityEventApiIntegrationTest {

        //replace real coordinates with house coordinates
    private static final String VALID_EVENT = """
            {
              "eventId": "evt-1001",
              "deviceId": "esp32-front-door",
              "eventType": "MOTION_DETECTED",
              "locationName": "Front Door",
              "latitude": 39.7392,
              "longitude": -104.9903,
              "armed": true,
              "timestamp": "2026-07-18T12:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityEventRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndListsAnEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1001"))
                .andExpect(jsonPath("$.eventType").value("MOTION_DETECTED"));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("esp32-front-door"));
    }

    @Test
    void listsNewestEventsFirst() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                .content(VALID_EVENT)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                .content(VALID_EVENT.replace("evt-1001", "evt-1002")
                        .replace("2026-07-18T12:00:00Z", "2026-07-18T13:00:00Z")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events"))
                .andExpect(jsonPath("$[0].eventId").value("evt-1002"))
                .andExpect(jsonPath("$[1].eventId").value("evt-1001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"latitude\": 91", "\"latitude\": -91",
            "\"longitude\": 181", "\"longitude\": -181"
    })
    void rejectsInvalidCoordinates(String replacement) throws Exception {
        String field = replacement.contains("latitude") ? "\"latitude\": 39.7392" : "\"longitude\": -104.9903";
        String fieldName = replacement.contains("latitude") ? "latitude" : "longitude";
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT.replace(field, replacement)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors." + fieldName).exists());
    }

    @Test
    void rejectsMissingRequiredField() throws Exception {
        String missingDevice = VALID_EVENT.replace("\"deviceId\": \"esp32-front-door\",", "");
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(missingDevice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.deviceId").exists());
    }

    @Test
    void rejectsUnknownEventType() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT.replace("MOTION_DETECTED", "WINDOW_BROKEN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unknown eventType")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": \"broken\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void rejectsDuplicateEventId() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_EVENT))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_EVENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("evt-1001")));
    }

    @Test
    void returnsCursorOnTargetXmlForStoredEvent() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(VALID_EVENT))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/evt-1001/cot"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<event version=\"2.0\"")))
                .andExpect(content().string(containsString("eventType=\"MOTION_DETECTED\"")))
                .andExpect(content().string(containsString("lat=\"39.7392\"")));
    }

    @Test
    void returnsNotFoundForMissingCotEvent() throws Exception {
        mockMvc.perform(get("/api/events/missing/cot"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Security event not found: missing"));
    }
}
