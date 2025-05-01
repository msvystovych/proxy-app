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

/*
  ProxyService is responsible for fetching content from an external service.
  It uses WebClient to make HTTP GET requests and handles the response status codes.
  If the response indicates a client-side (4xx) or server-side (5xx) error,
  it logs the error and throws a custom exception.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final WebClient proxyWebClient;

    /**
     * Fetches the content from an external service via the specified path.
     * Handles response status codes for client-side and server-side errors, logging appropriately
     * and throwing custom exceptions when such errors are encountered.
     *
     * @param path the URL path to fetch the content from
     * @return a {@code Mono<String>} emitting the content of the response body on success
     *         or propagating an error if the external service returns 4xx or 5xx status codes
     */
    public Mono<String> fetchExternalContent(String path) {
        return proxyWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
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
                .bodyToMono(String.class)
                .doOnNext(html -> log.info("Fetched content: {}", html.substring(0, Math.min(200, html.length()))));
    }
}
