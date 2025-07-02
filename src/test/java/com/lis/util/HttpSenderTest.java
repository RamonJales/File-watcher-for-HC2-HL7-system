package com.lis.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class HttpSenderTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp() {
        // Initialize WireMock server on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testSendToServerSuccess() throws Exception {
        // Arrange: Mock a successful response
        String url = "http://localhost:" + wireMockServer.port() + "/test";
        String content = "Hello, Server!";
        String expectedResponse = "Success response";

        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(equalTo(content))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(expectedResponse)));

        // Act
        HttpSender.sendToServer(url, content);

        // Assert: Verify the request was made
        verify(postRequestedFor(urlEqualTo("/test"))
                .withRequestBody(equalTo(content)));
    }

    @Test
    void testSendToServerErrorResponse() {
        // Arrange: Mock an error response
        String url = "http://localhost:" + wireMockServer.port() + "/error";
        String content = "Test content";
        String errorResponse = "Bad Request";

        stubFor(post(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(errorResponse)));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            HttpSender.sendToServer(url, content);
        });

        assertTrue(exception.getMessage().contains("Server returned error: 400"));
        assertTrue(exception.getMessage().contains(errorResponse));
    }

    @Test
    void testSendToServerInvalidUrl() {
        // Arrange: Use an invalid URL
        String invalidUrl = "http://nonexistent-server:9999/test";
        String content = "Test content";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            HttpSender.sendToServer(invalidUrl, content);
        });

        assertNotNull(exception);
    }
}