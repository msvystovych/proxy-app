package org.company.controller;

import org.company.model.CachedPage;
import org.company.repository.CachedPageRepository;
import org.company.repository.RequestResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
public class ProxyControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CachedPageRepository cachedPageRepository;

    @Autowired
    private RequestResponseRepository requestResponseRepository;

    @BeforeEach
    void setup() {
        cachedPageRepository.deleteAll().block(); // clean MongoDB cache
        requestResponseRepository.deleteAll();    // clean Postgres requests
    }

    @Test
    void testProxyRootPage() {
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).contains("Spring");
                });
    }

    @Test
    void testProxySubpage() {
        webTestClient.get()
                .uri("/proxy/why-spring/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).contains("Spring");
                });
    }

    @Test
    void testCachingMechanism() {
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk();

        CachedPage page = cachedPageRepository.findById("/").block();
        assertThat(page).isNotNull();

        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).isNotNull();
                });
    }

    @Test
    void testInternalLinksRewriting() {
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).contains("href=\"/proxy/");
                });
    }

    @Test
    void testSixLetterWordsTrademarked() {
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertThat(body).isNotNull();

                    if (!body.contains("™")) {
                        System.out.println("WARNING: No ™ found in body. Page may not contain six-letter words.");
                        System.out.println(body.substring(0, Math.min(body.length(), 1000)));
                    }

                    assertThat(body).contains("™");
                });
    }

    @Test
    void testNonExistentPathReturns404OrHandledGracefully() {
        webTestClient.get()
                .uri("/proxy/thispage/doesnotexist")
                .exchange()
                .expectStatus().isNotFound(); // 404
    }
}