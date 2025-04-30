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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@Testcontainers
public class ProxyControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("proxydb")
            .withUsername("proxyuser")
            .withPassword("proxypass");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    private WebTestClient webTestClient;

    @Autowired
    private CachedPageRepository cachedPageRepository;

    @Autowired
    private RequestResponseRepository requestResponseRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        cachedPageRepository.deleteAll().block(); // Clean MongoDB cache
        requestResponseRepository.deleteAll();    // Clean Postgres requests

        int bufferSize = 4 * 1024 * 1024; // 4MB

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .exchangeStrategies(strategies)
                .build();
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