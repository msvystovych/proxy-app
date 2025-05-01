package org.company.service;

import org.company.BaseIntegrationTest;
import org.company.repository.CachedPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
public class CacheServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CachedPageRepository cachedPageRepository;

    private final String path = "/test-cache-path";
    private final String html = "<html><body><h1>Test™ Page</h1></body></html>";

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