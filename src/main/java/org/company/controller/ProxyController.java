package org.company.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.exception.InternalProxyException;
import org.company.exception.UpstreamClientException;
import org.company.exception.UpstreamServerException;
import org.company.model.RequestResponseEntity;
import org.company.repository.RequestResponseRepository;
import org.company.service.CacheService;
import org.company.service.HtmlModifierService;
import org.company.service.ProxyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/proxy")
public class ProxyController {

    private final ProxyService proxyService;
    private final HtmlModifierService htmlModifierService;
    private final CacheService cacheService;
    private final RequestResponseRepository requestResponseRepository;

    @GetMapping("/**")
    public Mono<String> proxy(ServerWebExchange exchange, @RequestHeader Map<String, String> headers) {
        String path = extractPath(exchange);

        log.info("Received proxy request for path: {}", path);

        return cacheService.getCachedPage(path).switchIfEmpty(proxyService.fetchExternalContent(path).flatMap(originalHtml -> {
            String modifiedHtml = htmlModifierService.modifyHtml(originalHtml, path);
            cacheService.cachePage(path, modifiedHtml);
            saveRequestResponse(path, headers, modifiedHtml);
            return Mono.just(modifiedHtml);
        }).onErrorResume(ex -> {
            if (ex instanceof UpstreamClientException ||
                    ex instanceof UpstreamServerException) {
                return Mono.error(ex); // propagate upstream status
            }
            log.error("Unhandled proxy error for path {}: {}", path, ex.getMessage());
            return Mono.error(new InternalProxyException("Internal proxy error", ex));
        }));
    }

    private void saveRequestResponse(String path, Map<String, String> headers, String responseBody) {
        RequestResponseEntity entity = new RequestResponseEntity();
        entity.setUrl(path);
        entity.setRequestHeaders(headers.toString());
        entity.setResponseBody(responseBody);
        entity.setTimestamp(Instant.now());
        requestResponseRepository.save(entity);
    }

    private String extractPath(ServerWebExchange exchange) {
        String fullPath = exchange.getRequest().getURI().getPath(); // e.g., /proxy/spring3/
        String contextPath = exchange.getRequest().getPath().contextPath().value(); // usually ""

        String mappingPath = "/proxy";

        if (fullPath.startsWith(contextPath + mappingPath)) {
            String extracted = fullPath.substring((contextPath + mappingPath).length());
            return extracted.isEmpty() ? "/" : extracted;
        } else {
            return "/";
        }
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public static class ProxyException extends RuntimeException {
        public ProxyException(String message) {
            super(message);
        }
    }
}