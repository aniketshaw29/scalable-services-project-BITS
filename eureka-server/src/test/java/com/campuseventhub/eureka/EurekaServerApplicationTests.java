package com.campuseventhub.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EurekaServerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }

    @Test
    void actuatorHealthReturnsUp() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void eurekaDashboardIsAccessible() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/", String.class);

        // Eureka dashboard returns 200 and contains the UI
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eurekaAppsEndpointIsAccessible() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("http://localhost:" + port + "/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
