package org.company.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient proxyWebClient() {
        int bufferSize = 4 * 1024 * 1024; // 4MB

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(bufferSize))
                .build();

        return WebClient.builder()
                .baseUrl("https://spring.io")
                .exchangeStrategies(strategies)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; ProxyBot/1.0)")
                .build();
    }
}