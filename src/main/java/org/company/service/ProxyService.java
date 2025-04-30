package org.company.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.exception.UpstreamClientException;
import org.company.exception.UpstreamServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final WebClient proxyWebClient;

    /**
     * Fetch HTML page from upstream.
     */
    public Mono<String> fetchExternalContent(String path) {
        return proxyWebClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                            HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                            log.warn("Upstream returned 4xx for path {}: {}", path, status);
                            return Mono.error(new UpstreamClientException(status, "Upstream returned 4xx"));
                        })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                            HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                            log.error("Upstream returned 5xx for path {}: {}", path, status);
                            return Mono.error(new UpstreamServerException(status, "Upstream returned 5xx"));
                        })
                .bodyToMono(String.class);
    }

    /**
     * Simple file-type filter for static resources.
     */
    public boolean isStaticResource(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.matches(".*\\.(js|css|png|jpg|jpeg|svg|ico|gif|woff2?|ttf|eot|otf)$");
    }
}
