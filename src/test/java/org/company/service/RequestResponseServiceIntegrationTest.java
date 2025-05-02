package org.company.service;

import org.company.repository.RequestResponseRepository;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@Testcontainers
public class RequestResponseServiceIntegrationTest {

    @Autowired
    private RequestResponseService requestResponseService;

    @Autowired
    private RequestResponseRepository requestResponseRepository;

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

    private final String path = "/proxy/test";
    private final Map<String, String> headers = Map.of(
            "User-Agent", "JUnit",
            "Accept", "text/html"
    );
    private final String responseBody = "<html><body>Test™</body></html>";

    @BeforeEach
    void setup() {
        requestResponseRepository.deleteAll(); // blocking call, fine here for integration test
    }

    @Test
    void testSaveRequestResponse() {
        StepVerifier.create(requestResponseService.saveRequestResponse(path, headers, responseBody))
                .assertNext(saved -> {
                    assertThat(saved.getUrl()).isEqualTo(path);
                    assertThat(saved.getRequestHeaders()).contains("User-Agent=JUnit");
                    assertThat(saved.getResponseBody()).isEqualTo(responseBody);
                    assertThat(saved.getTimestamp()).isNotNull();
                })
                .verifyComplete();

        var all = requestResponseRepository.findAll();
        assertThat(all).hasSize(1);
    }
}