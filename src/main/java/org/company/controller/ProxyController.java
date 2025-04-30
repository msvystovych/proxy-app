package org.company.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.exception.InternalProxyException;
import org.company.exception.UpstreamClientException;
import org.company.exception.UpstreamServerException;
import org.company.service.CacheService;
import org.company.service.HtmlModifierService;
import org.company.service.ProxyService;
import org.company.service.RequestResponseService;
import org.company.util.UrlUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * ProxyController handles incoming requests and proxies them to an external service.
 * It modifies the HTML response, caches it, and saves request/response data.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/proxy")
public class ProxyController {

    private final ProxyService proxyService;
    private final HtmlModifierService htmlModifierService;
    private final CacheService cacheService;
    private final RequestResponseService requestResponseService;


    @GetMapping("/**")
    public Mono<String> proxy(ServerWebExchange exchange, @RequestHeader Map<String, String> headers) {
        String path = UrlUtils.extractPath(exchange);

        log.info("Received proxy request for path: {}", path);

        return cacheService.getCachedPage(path).switchIfEmpty(proxyService.fetchExternalContent(path).flatMap(originalHtml -> {
            String modifiedHtml = htmlModifierService.modifyHtml(originalHtml);
            cacheService.cachePage(path, modifiedHtml);
            requestResponseService.saveRequestResponse(path, headers, modifiedHtml);
            return Mono.just(modifiedHtml);
        }).onErrorResume(ex -> {
            if (ex instanceof UpstreamClientException || ex instanceof UpstreamServerException) {
                return Mono.error(ex); // propagate upstream status
            }
            log.error("Unhandled proxy error for path {}: {}", path, ex.getMessage());
            return Mono.error(new InternalProxyException("Internal proxy error", ex));
        }));
    }



}