package org.company.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.model.CachedPage;
import org.company.repository.CachedPageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CachedPageRepository cachedPageRepository;

    /**
     * Retrieves the cached modified HTML for the given path if available.
     *
     * @param path the URL path (e.g., "/spring3/")
     * @return Mono of modified HTML if cache hit, empty if cache miss
     */
    public Mono<String> getCachedPage(String path) {
        return cachedPageRepository.findById(path)
                .map(CachedPage::getModifiedHtml)
                .doOnNext(html -> log.info("Cache hit for path: {}", path))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Cache miss for path: {}", path);
                    return Mono.empty();
                }));
    }

    /**
     * Saves the modified HTML content into cache with TTL of 10 minutes.
     *
     * @param path         the URL path (e.g., "/spring3/")
     * @param modifiedHtml the modified HTML content
     * @return Mono<Void> to allow reactive chaining
     */
    public Mono<Void> cachePage(String path, String modifiedHtml) {
        CachedPage page = new CachedPage();
        page.setUrl(path);
        page.setModifiedHtml(modifiedHtml);
        page.setCreatedAt(Instant.now());

        return cachedPageRepository.save(page)
                .doOnSuccess(saved -> log.info("Cached page for path: {}", path))
                .doOnError(error -> log.error("Failed to cache page for path: {}", path, error))
                .then();
    }

    /**
     * Fire-and-forget call for production use
     */
    public void cachePageAsync(String path, String modifiedHtml) {
        cachePage(path, modifiedHtml).subscribe();
    }
}