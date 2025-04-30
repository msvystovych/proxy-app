package org.company.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProxyService {

    private final WebClient webClient;

    public ProxyService(@Qualifier("proxyWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches HTML page from upstream as String
     */
    public Mono<String> fetchExternalContent(String path) {
        return webClient.get().uri(path).accept(MediaType.TEXT_HTML).retrieve().onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
            log.warn("4xx error received from upstream for path: {}", path);
            return Mono.error(new UpstreamException("Upstream 4xx for path: " + path));
        }).onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
            log.error("5xx error received from upstream for path: {}", path);
            return Mono.error(new UpstreamException("Upstream 5xx for path: " + path));
        }).bodyToMono(String.class).doOnError(WebClientResponseException.class, ex -> {
            log.error("WebClient error when fetching path: {}, status: {}", path, ex.getStatusCode(), ex);
        }).doOnError(ex -> {
            log.error("Unexpected error fetching path: {}", path, ex);
        });
    }

    /**
     * Fetches any static resource from upstream (images, js, css) as raw DataBuffer
     */
    public Mono<DataBuffer> fetchStaticResource(String path) {
        return webClient.get().uri(path).accept(MediaType.ALL).retrieve().onStatus(statusCode -> statusCode.is4xxClientError(), clientResponse -> {
            log.warn("4xx error received for static resource: {}", path);
            return Mono.error(new UpstreamException("Upstream 4xx for static resource: " + path));
        }).onStatus(statusCode -> statusCode.is5xxServerError(), clientResponse -> {
            log.error("5xx error received for static resource: {}", path);
            return Mono.error(new UpstreamException("Upstream 5xx for static resource: " + path));
        }).bodyToMono(DataBuffer.class).doOnError(WebClientResponseException.class, ex -> {
            log.error("WebClient error for static resource: {}, status: {}", path, ex.getStatusCode(), ex);
        }).doOnError(ex -> {
            log.error("Unexpected error fetching static resource: {}", path, ex);
        });
    }

    /**
     * Simple utility to detect if a path is "probably" static content
     */
    public boolean isStaticResource(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".js") || lower.endsWith(".css") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.endsWith(".eot") || lower.endsWith(".otf");
    }

    public static class UpstreamException extends RuntimeException {
        public UpstreamException(String message) {
            super(message);
        }
    }
}
