package org.company.service;

import org.company.repository.CachedPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@Testcontainers
public class CacheServiceIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CachedPageRepository cachedPageRepository;

    private final String path = "/test-cache-path";
    private final String html = "<html><body><h1>Test™ Page</h1></body></html>";


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

    @BeforeEach
    void cleanCache() {
        cachedPageRepository.deleteAll().block();
    }

    @Test
    void testCacheMissThenWriteAndReadBack() {
        // Step 1: verify cache miss
        StepVerifier.create(cacheService.getCachedPage(path))
                .expectNextCount(0)
                .verifyComplete();

        // Step 2: write to cache
        cacheService.cachePage(path, html).block();

        // Step 3: verify cache hit
        StepVerifier.create(cacheService.getCachedPage(path))
                .expectNext(html)
                .verifyComplete();

        // Optional: inspect metadata
        StepVerifier.create(cachedPageRepository.findById(path))
                .assertNext(cachedPage -> {
                    assertThat(cachedPage.getModifiedHtml()).isEqualTo(html);
                    assertThat(cachedPage.getUrl()).isEqualTo(path);
                    assertThat(cachedPage.getCreatedAt()).isNotNull();
                })
                .verifyComplete();
    }
}