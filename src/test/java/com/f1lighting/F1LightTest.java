package com.f1lighting;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest // Spins up the temporary server automatically
public class F1LightTest {

    @Test
    public void testSuccessfulLeadChangeParsesCorrectly(WireMockRuntimeInfo wmRuntimeInfo) {
        // 1. Arrange: Point the controller to WireMock's dynamic port
        String mockApiBaseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/v1";

        // Fake the position endpoint returning Charles Leclerc (16)
        stubFor(get(urlEqualTo("/v1/position?session_key=latest&position=1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"driver_number\": 16, \"position\": 1, \"session_key\": \"latest\"}]")));

        // Fake the drivers endpoint resolving 16 to Ferrari
        stubFor(get(urlEqualTo("/v1/drivers?session_key=latest&driver_number=16"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"driver_number\": 16, \"team_name\": \"Ferrari\"}]")));

        Main controller = new Main(mockApiBaseUrl);

        // 2. Act: Trigger exactly one polling cycle
        String detectedTeam = controller.pollAndUpdate();

        // 3. Assert: Ensure the Java object traversed the JSON successfully
        assertEquals("Ferrari", detectedTeam);
    }

    @Test
    public void testEmptyApiResponseIsHandledGracefully(WireMockRuntimeInfo wmRuntimeInfo) {
        String mockApiBaseUrl = wmRuntimeInfo.getHttpBaseUrl() + "/v1";

        // Simulating a cold session with no track data
        stubFor(get(urlEqualTo("/v1/position?session_key=latest&position=1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]"))); // Empty JSON Array

        Main controller = new Main(mockApiBaseUrl);
        String detectedTeam = controller.pollAndUpdate();

        assertEquals(null, detectedTeam);
    }
}