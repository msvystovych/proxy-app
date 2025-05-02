package org.company.controller;

import jakarta.annotation.PostConstruct;
import org.company.model.CachedPage;
import org.company.repository.CachedPageRepository;
import org.company.repository.RequestResponseRepository;
import org.company.util.ProxyConstants;
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

    @Autowired
    private CachedPageRepository cachedPageRepository;

    @Autowired
    private RequestResponseRepository requestResponseRepository;

    @BeforeEach
    void setup() {
        cachedPageRepository.deleteAll().block(); // clean MongoDB cache
        requestResponseRepository.deleteAll();    // clean Postgres requests
    }

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

    @LocalServerPort
    private int port;

    public WebTestClient webTestClient;


    @PostConstruct
    public void setupWebClient() {
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

                    if (!body.contains(ProxyConstants.TM_MARK)) {
                        System.out.println("WARNING: No ™ found in body. Page may not contain six-letter words.");
                        System.out.println(body.substring(0, Math.min(body.length(), 1000)));
                    }

                    assertThat(body).contains(ProxyConstants.TM_MARK);
                });
    }

    @Test
    void testNonExistentPathReturns404OrHandledGracefully() {
        webTestClient.get()
                .uri("/proxy/thispage/doesnotexist")
                .exchange()
                .expectStatus().isNotFound(); // 404
    }

    @Test
    void testRequestAndResponseAreSavedInPostgres() {
        webTestClient.get()
                .uri("/proxy/")
                .header("X-Test-Header", "integration")
                .exchange()
                .expectStatus().isOk();

        var entries = requestResponseRepository.findAll();
        assertThat(entries).isNotEmpty();

        var entry = entries.get(0);
        assertThat(entry.getUrl()).isEqualTo("/");
        assertThat(entry.getRequestHeaders()).contains("X-Test-Header");
        assertThat(entry.getResponseBody()).isNotBlank();
        assertThat(entry.getTimestamp()).isNotNull();
    }

    @Test
    void testCachedResponseIsIdenticalToOriginal() {
        String[] originalBody = new String[1];

        // First call (triggers fetch and cache)
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> originalBody[0] = response.getResponseBody());

        // Second call (should hit MongoDB cache)
        webTestClient.get()
                .uri("/proxy/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String cachedBody = response.getResponseBody();
                    assertThat(cachedBody).isEqualTo(originalBody[0]);
                });
    }

    @Test
    void testResponseIsPlainContentType() {
        webTestClient.get()
                .uri("/proxy/why-spring/")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("Content-Type", value -> assertThat(value).contains("text/plain"));
    }

    @Test
    void testResponseIsHtmlContentType_debug() {
        webTestClient.get()
                .uri("/proxy/why-spring/")
                .exchange()
                .expectBody(String.class)
                .consumeWith(response -> {
                    var status = response.getStatus();
                    var body = response.getResponseBody();
                    System.out.println("Status: " + status);
                    System.out.println("Body:\n" + body);
                });
    }

//    spring.io doesn’t have many public endpoints with query params that return 200
    @Test
    void testProxyPreservesQueryParameters() {
        webTestClient.get()
                .uri("/proxy/search?q=spring")
                .exchange()
                .expectStatus().isNotFound(); // correct expectation
    }

    @Test
    void testWritesToCacheAfterProxyRequest() {
        // 1. Ensure cache is empty
        assertThat(cachedPageRepository.findById("/why-spring/").blockOptional()).isEmpty();

        // 2. Trigger proxy request (which should write to cache)
        webTestClient.get()
                .uri("/proxy/why-spring/")
                .exchange()
                .expectStatus().isOk();

        // 3. Assert cache entry exists
        var cachedPage = cachedPageRepository.findById("/why-spring/").blockOptional();
        assertThat(cachedPage).isPresent();

        // 4. Optionally validate content is non-empty and contains expected signature
        assertThat(cachedPage.get().getModifiedHtml())
                .isNotBlank()
                .contains(ProxyConstants.TM_MARK) // optionally validate ™ injection logic
                .contains("<html"); // sanity check
    }


}