package com.nipunapps.cardgame.security;

import com.nipunapps.cardgame.testconfig.AbstractContainerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class SecurityConfigIntegrationTest extends AbstractContainerProperties {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void whenAccessingPublicUrlWithoutAuth_thenOk() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void whenAccessingProtectedUrlWithoutAuth_thenUnauthorized() {
        webTestClient.get()
                .uri("/user/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void whenCorsRequestFromAllowedOrigin_thenHeadersArePresent() {
        webTestClient.get()
                .uri("/")
                .header("Origin", "http://localhost:5173")
                .exchange()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    // Additional tests for authentication success, failure, session concurrency etc. can be added here
}
