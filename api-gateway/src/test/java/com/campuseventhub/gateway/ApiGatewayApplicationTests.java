package com.campuseventhub.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }

    @Test
    void actuatorHealthReturnsUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("UP"));
    }

    @Test
    void actuatorInfoIsAccessible() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRouteReturns404() {
        // Routes with no upstream service return 503/404 from the gateway itself
        webTestClient.get()
                .uri("/api/nonexistent-service/test")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isIn(
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.SERVICE_UNAVAILABLE.value()
                        )
                );
    }
}
